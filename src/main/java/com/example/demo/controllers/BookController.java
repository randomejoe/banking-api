package com.example.demo.controllers;

import com.example.demo.models.BookModel;
import com.example.demo.services.BookService;
import org.springframework.web.bind.annotation.*;

/*
 * @RestController is a Spring Boot annotation used to create RESTful web services.
 *
 * It combines two annotations:
 * - @Controller → marks the class as a web controller
 * - @ResponseBody → tells Spring to return data (like JSON or XML) directly
 *   instead of rendering a webpage (HTML)
 *
 * This means:
 * - Methods in this class handle HTTP requests (GET, POST, PUT, DELETE, etc.)
 * - The return values of these methods are automatically converted into JSON (by default)
 *
 * In short:
 * @RestController = "This class handles web requests and returns data directly."
 */
@RestController
@RequestMapping("books")
public class BookController {

    private BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // Get all books, long form request mapping
    @RequestMapping(value = "", method = RequestMethod.GET)
    Iterable<BookModel> getAll() {
        return this.bookService.getBooks();
    }

    // get book by ID
    @GetMapping("/{id}")
    // type path param gets injected directly in the controller param using @PathVariable
    BookModel get(@PathVariable int id) {
        return this.bookService.getBookById(id);
    }

    // create a book
    @PostMapping("")
    // Request body automatically maps post data to book model
    BookModel create(@RequestBody BookModel book) {
        return this.bookService.create(book);
    }

    // update a book
    @PutMapping("")
    // Request body automatically maps post data to book model
    BookModel update(@RequestBody BookModel book) {
        return this.bookService.update(book);
    }

    // delete book by ID
    @DeleteMapping("/{id}")
    Object delete(@PathVariable int id) {
        this.bookService.delete(id);
        return new Object();
    }

}
