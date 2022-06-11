package com.yahoo.behaviorgraph

fun interface Thunk {
    fun invoke();
}

fun interface ExtentThunk<T> {
    fun invoke(ctx: T)
}