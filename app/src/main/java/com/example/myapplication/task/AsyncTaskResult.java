package com.example.myapplication.task;

public class AsyncTaskResult<T> {
    private T mResult;
    private Throwable mError;

    public AsyncTaskResult(T result) {
        this.mResult = result;
    }

    public AsyncTaskResult(Throwable error) {
        this.mError = error;
    }

    public T getResult() {
        return mResult;
    }

    public Throwable getError() {
        return mError;
    }

    public boolean hasError() {
        return (mError != null);
    }

    public boolean hasResult() {
        return (mResult != null);
    }
}