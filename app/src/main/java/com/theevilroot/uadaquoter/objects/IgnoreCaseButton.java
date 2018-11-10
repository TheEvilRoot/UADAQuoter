package com.theevilroot.uadaquoter.objects;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;

public class IgnoreCaseButton extends AppCompatImageButton {

    public Boolean value;

    public IgnoreCaseButton(Context context) {
        super(context);
        value = true;
    }

    public IgnoreCaseButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        value = true;
    }

    public IgnoreCaseButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        value = true;
    }

    public void setIgnoreCase(Boolean vl) {
        value = vl;
        updateDrawable();
    }

    public void turnIgnoreCase() {
        setIgnoreCase(!value);
    }

    public void updateDrawable() {
        if(value)
            setColorFilter(Color.argb(255,255,255,255));
        else setColorFilter(Color.argb(50,255,255,255));
    }

}
