package com.casaempresario.app.repository;

public interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(Exception e);
}
