package ru.akirakozov.sd.refactoring.servlet;

import java.util.Objects;

class Product {
    final String name;
    final int price;

    Product(String name, int price) {
        this.name = name;
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Product) {
            Product product = (Product) o;
            return price == product.price && name.equals(product.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, price);
    }

    @Override
    public String toString() {
        return name + "\t" + price;
    }
}
