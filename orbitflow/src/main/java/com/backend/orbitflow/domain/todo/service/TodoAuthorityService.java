package com.backend.orbitflow.domain.todo.service;

import com.backend.orbitflow.domain.category.entity.Category;
import com.backend.orbitflow.domain.todo.entity.Todo;
import com.backend.orbitflow.domain.user.entity.User;

public interface TodoAuthorityService {

    void checkView(Category category, User viewer);
    void checkEdit(Category category, User actor);
    void checkComplete(Todo todo, User actor);
    void checkAssign(Category category, User actor, User assignee);
}
