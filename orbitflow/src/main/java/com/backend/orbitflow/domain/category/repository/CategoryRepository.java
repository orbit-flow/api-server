package com.backend.orbitflow.domain.category.repository;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.team.entity.Team;
import com.backend.orbitflow.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("select c from Category c left join fetch c.user left join fetch c.team where c.id = :id")
    Optional<Category> findWithOwnerById(@Param("id") Long id);

    List<Category> findAllByUserOrderByCreatedAtAsc(User user);

    List<Category> findAllByTeamOrderByCreatedAtAsc(Team team);
}
