package me.jansv.lintsample;

import javax.inject.Inject;

import me.jansv.internallib.UtilityClass;


public class SomeJavaClass {
    @Inject
    public UtilityClass utilityClass;

    @Inject
    public KotlinInterface kotlinInterface;

    @Inject
    public JavaInterface javaInterface;
}
