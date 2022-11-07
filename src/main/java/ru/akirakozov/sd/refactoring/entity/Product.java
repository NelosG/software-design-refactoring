package ru.akirakozov.sd.refactoring.entity;

import java.util.Objects;

public class Product {
    private final Integer id;
    private final String name;
    private final int price;

    public Product(Integer id, String name, int price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public Product(String name, int price) {
        this(null, name, price);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Product) {
            Product product = (Product) o;
            return Objects.equals(id, product.id) &&
                    price == product.price &&
                    name.equals(product.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price);
    }

    @Override
    public String toString() {
        return "Product {" +
                "id=" + id +
                ", name='" + name + "'" +
                ", price=" + price +
                '}';
    }
}
