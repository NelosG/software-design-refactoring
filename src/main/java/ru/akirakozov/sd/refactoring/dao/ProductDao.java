package ru.akirakozov.sd.refactoring.dao;


import ru.akirakozov.sd.refactoring.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductDao {
    void insert(Product product);

    List<Product> findAll();

    int findCount();

    long findSumPrice();

    Optional<Product> findWithMaxPrice();

    Optional<Product> findWithMinPrice();
}
