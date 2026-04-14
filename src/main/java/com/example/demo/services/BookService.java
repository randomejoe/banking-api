package com.example.demo.services;

import com.example.demo.models.BookModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookService {
    private List<BookModel> books = new ArrayList<BookModel>();
    private int currentId = 2;

    public BookService() {
        this.books.add(new BookModel(1, "Catcher in the rye", "J.D. Salinger"));
        this.books.add(new BookModel(2, "Crime and Punishment", "Fyodor Dostoevsky"));
    }

    public List<BookModel> getBooks() {
        return this.books;
    }

    public BookModel getBookById(int id) {
        return books.stream().filter(book -> book.getId() == id).findFirst().orElse(null);
    }

    public BookModel create(BookModel book) {
        currentId++;
        book.setId(currentId);
        this.books.add(book);
        return book;
    }

    public BookModel update(BookModel book) {
        this.books = this.books.stream().map(b -> {
            if(b.getId() == book.getId()){
                return book;
            }
            return b;
        }).toList();
        return book;
    }

    public void delete(int id) {
        this.books = this.books.stream().filter(b -> {
            return b.getId() != id;
        }).toList();
    }
}
