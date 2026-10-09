package com.backend.orbitflow.domain.item.service;

import com.backend.orbitflow.domain.avatar.repository.UserItemRepository;
import com.backend.orbitflow.domain.avatar.service.AvatarService;
import com.backend.orbitflow.domain.item.dto.response.ItemResponse;
import com.backend.orbitflow.domain.item.entity.Item;
import com.backend.orbitflow.domain.item.enums.ItemType;
import com.backend.orbitflow.domain.item.error.ItemErrorCode;
import com.backend.orbitflow.domain.item.repository.ItemRepository;
import com.backend.orbitflow.domain.user.entity.User;
import com.backend.orbitflow.global.common.error.exception.CommonException;
import com.backend.orbitflow.global.util.S3TransactionalFileManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceImpl implements ItemService {

    private static final String IMAGE_DIR = "items";

    private final ItemRepository itemRepository;
    private final AvatarService avatarService;
    private final UserItemRepository userItemRepository;
    private final S3TransactionalFileManager s3FileManager;

    // 판매 중인 아이템과 요청자의 보유 여부
    @Transactional(readOnly = true)
    public Page<ItemResponse> getShopItems(User me, ItemType type, int page, int size) {
        Page<Item> items = itemRepository.findOnSale(type, toPageable(page, size));
        Set<Long> ownedIds = avatarService.findOwnedItemIds(me, items.getContent());
        return items.map(item -> ItemResponse.of(item, ownedIds.contains(item.getId())));
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> searchItems(ItemType type, Boolean onSale, int page, int size) {
        return itemRepository.search(type, onSale, toPageable(page, size))
                .map(ItemResponse::from);
    }

    public ItemResponse createItem(String name, ItemType type, int price, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CommonException(ItemErrorCode.ITEM_IMAGE_REQUIRED);
        }
        String imageUrl = s3FileManager.upload(IMAGE_DIR, image);
        return ItemResponse.from(itemRepository.save(Item.of(name, type, price, imageUrl)));
    }

    // 이미지를 보내지 않으면 기존 이미지 유지, 교체 시 기존 이미지는 커밋 후 삭제
    // 가격 변경은 이후 구매부터 적용 (구매 시점의 현재 가격으로 차감)
    public ItemResponse updateItem(Long itemId, String name, ItemType type, int price, MultipartFile image) {
        Item item = getItem(itemId);
        if (item.getType() != type) {
            // 다른 부위로 바뀌면 같은 부위 중복 장착이 생길 수 있으므로 기존 장착 해제
            userItemRepository.unequipAllByItem(item);
            item = getItem(itemId);
        }
        item.updateItem(name, type, price);
        if (image != null && !image.isEmpty()) {
            String oldUrl = item.getImageUrl();
            item.updateImageUrl(s3FileManager.upload(IMAGE_DIR, image));
            s3FileManager.deleteAfterCommit(List.of(oldUrl));
        }
        return ItemResponse.from(item);
    }

    // 판매 중지 시 신규 구매만 불가, 이미 보유한 사용자의 보유·장착 상태는 유지
    public ItemResponse updateOnSale(Long itemId, boolean onSale) {
        Item item = getItem(itemId);
        item.updateOnSale(onSale);
        return ItemResponse.from(item);
    }

    private Item getItem(Long itemId) {
        return itemRepository.findById(itemId).orElseThrow(
                () -> new CommonException(ItemErrorCode.ITEM_NOT_FOUND)
        );
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), size);
    }
}
