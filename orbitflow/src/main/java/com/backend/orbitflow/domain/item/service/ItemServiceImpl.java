package com.backend.orbitflow.domain.item.service;

import com.backend.orbitflow.domain.item.dto.response.ItemResponse;
import com.backend.orbitflow.domain.item.entity.Item;
import com.backend.orbitflow.domain.item.enums.ItemType;
import com.backend.orbitflow.domain.item.error.ItemErrorCode;
import com.backend.orbitflow.domain.item.repository.ItemRepository;
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

@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceImpl implements ItemService {

    private static final String IMAGE_DIR = "items";

    private final ItemRepository itemRepository;
    private final S3TransactionalFileManager s3FileManager;

    // 판매 중인 아이템
    @Transactional(readOnly = true)
    public Page<Item> getShopItems(ItemType type, int page, int size) {
        return itemRepository.findOnSale(type, toPageable(page, size));
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
    // 부위 변경 시 기존 장착 해제는 호출 측(ItemFacade)에서 먼저 수행
    public ItemResponse updateItem(Long itemId, String name, ItemType type, int price, MultipartFile image) {
        Item item = getItem(itemId);
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

    @Transactional(readOnly = true)
    public Item getItem(Long itemId) {
        return itemRepository.findById(itemId).orElseThrow(
                () -> new CommonException(ItemErrorCode.ITEM_NOT_FOUND)
        );
    }

    // 요청 page는 1부터 시작
    private Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));
    }
}
