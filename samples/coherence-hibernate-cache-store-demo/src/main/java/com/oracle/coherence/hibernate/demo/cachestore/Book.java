/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.hibernate.demo.cachestore;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;

@Entity
public class Book implements Serializable {

	@Id
	String isbn;

	@NotNull
	String title;

	public Book(String isbn, String title) {
		this.isbn = isbn;
		this.title = title;
	}

	public Book() {
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final Book book = (Book) o;
		return Objects.equals(this.isbn, book.isbn) && Objects.equals(this.title, book.title);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.isbn, this.title);
	}

	@Override
	public String toString() {
		return "Book{" +
				"isbn='" + this.isbn + '\'' +
				", title='" + this.title + '\'' +
				'}';
	}
}
