package science.jondubois.lnzlib;


import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;


public class LnzSlider extends DialogPreference implements SeekBar.OnSeekBarChangeListener
{
    private static final String androidns="http://schemas.android.com/apk/res/android";
    private static final String customns="http://schemas.android.com/apk/lib/science.jondubois.lnzlib";

    private SeekBar mSeekBar;
    private TextView mSplashText,mValueText;
    private Context mContext;

    private String mDialogMessage, mSuffix;
    private int mDefault, mMax, mMin, mValue = 0;

    public LnzSlider(Context context, AttributeSet attrs) {
        super(context,attrs);
        mContext = context;
        this.setNegativeButtonText("");
        this.setPositiveButtonText(R.string.done);


        mDialogMessage = attrs.getAttributeValue(androidns,"dialogMessage");
        mSuffix = loadString(attrs,androidns,"text");
        mMax = loadInt( attrs, androidns, "max", R.integer.defaultSpeed );


        mMin = loadInt( attrs, customns, "min", 555 );
        mDefault = loadInt(attrs, androidns, "defaultValue", 23231244 ) - mMin;

    }
    private int loadInt( AttributeSet attrs, String nameSpace, String key, int def ){
        int stringResId = attrs.getAttributeResourceValue(nameSpace, key, 0);
        if( stringResId != 0 )
            return mContext.getResources().getInteger(stringResId);
        else
            return attrs.getAttributeIntValue(nameSpace, key, def);

    }
    private String loadString( AttributeSet attrs, String nameSpace, String key ){
        int stringResId = attrs.getAttributeResourceValue(nameSpace, key, 0);
        if( stringResId != 0 )
            return mContext.getResources().getString(stringResId);
        else
            return attrs.getAttributeValue(nameSpace, key);

    }
    @Override
    protected View onCreateDialogView() {
        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(3, 3, 3, 3);

        mSplashText = new TextView(mContext);
        mSplashText.setGravity(Gravity.CENTER_HORIZONTAL);
        if (mDialogMessage != null)
            mSplashText.setText(mDialogMessage);
        layout.addView(mSplashText);

        mValueText = new TextView(mContext);
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueText.setTextSize(20);
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mValueText, params);

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (shouldPersist())
            mValue = getPersistedInt(mDefault);

        mSeekBar.setMax(mMax - mMin);
        mSeekBar.setProgress(mValue);
        return layout;
    }
    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mSeekBar.setMax(mMax - mMin);
        mSeekBar.setProgress(mValue);
    }
    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue)
    {
        super.onSetInitialValue(restore, defaultValue);
        if (restore)
            mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
        else
            mValue = (Integer)defaultValue;
    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch)
    {
        String t = String.valueOf(value+mMin);
        mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
        if (shouldPersist())
            persistInt(value);
        callChangeListener(new Integer(value));
    }
    public void onStartTrackingTouch(SeekBar seek) {}
    public void onStopTrackingTouch(SeekBar seek) {}

    public void setProgress(int progress) {
        mValue = progress;
        if (mSeekBar != null)
            mSeekBar.setProgress(progress);
    }
    public int getProgress() { return mValue; }
}

