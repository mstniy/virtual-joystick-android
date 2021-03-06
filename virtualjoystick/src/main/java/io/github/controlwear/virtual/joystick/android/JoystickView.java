package io.github.controlwear.virtual.joystick.android;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class JoystickView extends View
        implements
        Runnable {


    /*
    INTERFACES
    */


    /**
     * Interface definition for a callback to be invoked when a
     * JoystickView's button is moved
     */
    public interface OnMoveListener {

        /**
         * Called when a JoystickView's button has been moved
         * @param delX Displacement along the x axis relative to the center. Between -100 and 100.
         * @param delY Displacement along the y axis relative to the center. Between -100 and 100.
         */
        void onMove(int delX, int delY);
    }


    /**
     * Interface definition for a callback to be invoked when a JoystickView
     * is touched and held by multiple pointers.
     */
    public interface OnMultipleLongPressListener {
        /**
         * Called when a JoystickView has been touch and held enough time by multiple pointers.
         */
        void onMultipleLongPress();
    }


    /*
    CONSTANTS
    */

    /**
     * Default refresh rate as a time in milliseconds to send move values through callback
     */
    private static final int DEFAULT_LOOP_INTERVAL = 50; // in milliseconds

    /**
     * Used to allow a slight move without cancelling MultipleLongPress
     */
    private static final int MOVE_TOLERANCE = 10;

    /**
     * Default color for button
     */
    private static final int DEFAULT_COLOR_BUTTON = Color.BLACK;

    /**
     * Default color for border
     */
    private static final int DEFAULT_COLOR_BORDER = Color.TRANSPARENT;

    /**
     * Default alpha for border
     */
    private static final int DEFAULT_ALPHA_BORDER = 255;

    /**
     * Default background color
     */
    private static final int DEFAULT_BACKGROUND_COLOR = Color.TRANSPARENT;

    /**
     * Default View's size
     */
    private static final int DEFAULT_SIZE = 200;

    /**
     * Default border's width
     */
    private static final int DEFAULT_WIDTH_BORDER = 3;

    /**
     * Default behavior to fixed center (not auto-defined)
     */
    private static final boolean DEFAULT_FIXED_CENTER = true;


    /**
     * Default behavior to auto re-center button (automatically recenter the button)
     */
    private static final boolean DEFAULT_AUTO_RECENTER_BUTTON = true;


    /**
     * Default behavior to button stickToBorder (button stay on the border)
     */
    private static final boolean DEFAULT_BUTTON_STICK_TO_BORDER = false;


    // DRAWING
    private Paint mPaintCircleButton;
    private Paint mPaintBorder;
    private Paint mPaintBackground;

    private Paint mPaintBitmapButton;
    private Bitmap mButtonBitmap;


    /**
     * Ratio use to define the size of the button
     */
    private float mButtonSizeRatio;


    /**
     * Ratio use to define the size of the background
     *
     */
    private float mBackgroundSizeRatio;


    // COORDINATE
    private int mPosX = 0;
    private int mPosY = 0;
    private int mCenterX = 0;
    private int mCenterY = 0;

    private int mFixedCenterX = 0;
    private int mFixedCenterY = 0;

    /**
     * Used to adapt behavior whether it is auto-defined center (false) or fixed center (true)
     */
    private boolean mFixedCenter;


    /**
     * Used to adapt behavior whether the button is automatically re-centered in x- and y-axes (true)
     * when released or not (false)
     */
    private boolean mAutoReCenterButtonX, mAutoReCenterButtonY;


    /**
     * Used to enabled/disabled the Joystick. When disabled (enabled to false) the joystick button
     * can't move and onMove is not called.
     */
    private boolean mEnabled;

    /**
     * Whether the callback will be called even if the user is not touching the joystick
     */
    private boolean mAlwaysCall;


    // SIZE
    private int mButtonRadius;
    private int mBorderWidth, mBorderHeight;


    /**
     * Alpha of the border (to use when changing color dynamically)
     */
    private int mBorderAlpha;


    /**
     * Based on mBorderWidth/Height but a bit smaller (minus half the stroke size of the border)
     */
    private float mBackgroundWidth, mBackgroundHeight;


    /**
     * Listener used to dispatch OnMove event
     */
    private OnMoveListener mCallback;

    private long mLoopInterval = DEFAULT_LOOP_INTERVAL;
    private Thread mThread;


    /**
     * Listener used to dispatch MultipleLongPress event
     */
    private OnMultipleLongPressListener mOnMultipleLongPressListener;

    private final Handler mHandlerMultipleLongPress = new Handler();
    private Runnable mRunnableMultipleLongPress;
    private int mMoveTolerance;


    /**
     * Default value.
     * Both direction correspond to horizontal and vertical movement
     */
    public static int BUTTON_DIRECTION_BOTH = 0;

    /**
     * The allowed direction of the button is define by the value of this parameter:
     * - 1 for horizontal axis only
     * - 2 for vertical axis only
     * - 0 for both axes
     */
    private int mButtonDirection = 0;


    /*
    CONSTRUCTORS
     */


    /**
     * Simple constructor to use when creating a JoystickView from code.
     * Call another constructor passing null to Attribute.
     * @param context The Context the JoystickView is running in, through which it can
     *        access the current theme, resources, etc.
     */
    public JoystickView(Context context) {
        this(context, null);
    }


    public JoystickView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }


    /**
     * Constructor that is called when inflating a JoystickView from XML. This is called
     * when a JoystickView is being constructed from an XML file, supplying attributes
     * that were specified in the XML file.
     * @param context The Context the JoystickView is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the JoystickView.
     */
    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.JoystickView,
                0, 0
        );

        int buttonColor;
        int borderColor;
        int backgroundColor;
        int borderWidth;
        Drawable buttonDrawable;
        try {
            buttonColor = styledAttributes.getColor(R.styleable.JoystickView_JV_buttonColor, DEFAULT_COLOR_BUTTON);
            borderColor = styledAttributes.getColor(R.styleable.JoystickView_JV_borderColor, DEFAULT_COLOR_BORDER);
            mBorderAlpha = styledAttributes.getInt(R.styleable.JoystickView_JV_borderAlpha, DEFAULT_ALPHA_BORDER);
            backgroundColor = styledAttributes.getColor(R.styleable.JoystickView_JV_backgroundColor, DEFAULT_BACKGROUND_COLOR);
            borderWidth = styledAttributes.getDimensionPixelSize(R.styleable.JoystickView_JV_borderWidth, DEFAULT_WIDTH_BORDER);
            mFixedCenter = styledAttributes.getBoolean(R.styleable.JoystickView_JV_fixedCenter, DEFAULT_FIXED_CENTER);
            mAutoReCenterButtonX = styledAttributes.getBoolean(R.styleable.JoystickView_JV_autoReCenterButtonX, DEFAULT_AUTO_RECENTER_BUTTON);
            mAutoReCenterButtonY = styledAttributes.getBoolean(R.styleable.JoystickView_JV_autoReCenterButtonY, DEFAULT_AUTO_RECENTER_BUTTON);
            buttonDrawable = styledAttributes.getDrawable(R.styleable.JoystickView_JV_buttonImage);
            mEnabled = styledAttributes.getBoolean(R.styleable.JoystickView_JV_enabled, true);
            mButtonSizeRatio = styledAttributes.getFraction(R.styleable.JoystickView_JV_buttonSizeRatio, 1, 1, 0.25f);
            mBackgroundSizeRatio = styledAttributes.getFraction(R.styleable.JoystickView_JV_backgroundSizeRatio, 1, 1, 0.75f);
            mButtonDirection = styledAttributes.getInteger(R.styleable.JoystickView_JV_buttonDirection, BUTTON_DIRECTION_BOTH);
        } finally {
            styledAttributes.recycle();
        }

        // Initialize the drawing according to attributes

        mPaintCircleButton = new Paint();
        mPaintCircleButton.setAntiAlias(true);
        mPaintCircleButton.setColor(buttonColor);
        mPaintCircleButton.setStyle(Paint.Style.FILL);

        if (buttonDrawable != null) {
            if (buttonDrawable instanceof BitmapDrawable) {
                mButtonBitmap = ((BitmapDrawable) buttonDrawable).getBitmap();
                mPaintBitmapButton = new Paint();
            }
        }

        mPaintBorder = new Paint();
        mPaintBorder.setAntiAlias(true);
        mPaintBorder.setColor(borderColor);
        mPaintBorder.setStyle(Paint.Style.STROKE);
        mPaintBorder.setStrokeWidth(borderWidth);

        if (borderColor != Color.TRANSPARENT) {
            mPaintBorder.setAlpha(mBorderAlpha);
        }

        mPaintBackground = new Paint();
        mPaintBackground.setAntiAlias(true);
        mPaintBackground.setColor(backgroundColor);
        mPaintBackground.setStyle(Paint.Style.FILL);


        // Init Runnable for MultiLongPress

        mRunnableMultipleLongPress = new Runnable() {
            @Override
            public void run() {
                if (mOnMultipleLongPressListener != null)
                    mOnMultipleLongPressListener.onMultipleLongPress();
            }
        };
    }


    private void initPosition() {
        // get the center of view to position circle
        mFixedCenterX = mCenterX = mPosX = getWidth() / 2;
        mFixedCenterY = mCenterY = mPosY = getWidth() / 2;
    }


    /**
     * Draw the background, the border and the button
     * @param canvas the canvas on which the shapes will be drawn
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // Draw the background
        canvas.drawRect(mFixedCenterX - mBackgroundWidth/2, mFixedCenterY - mBackgroundHeight/2,mFixedCenterX + mBackgroundWidth/2, mFixedCenterY + mBackgroundHeight/2, mPaintBackground);

        // Draw the circle border
        canvas.drawRect(mFixedCenterX - mBorderWidth/2, mFixedCenterY - mBorderHeight/2,mFixedCenterX + mBorderWidth/2, mFixedCenterY + mBorderHeight/2, mPaintBorder);

        // Draw the button from image
        if (mButtonBitmap != null) {
            canvas.drawBitmap(
                    mButtonBitmap,
                    mPosX + mFixedCenterX - mCenterX - mButtonRadius,
                    mPosY + mFixedCenterY - mCenterY - mButtonRadius,
                    mPaintBitmapButton
            );
        }
        // Draw the button as simple circle
        else {
            canvas.drawCircle(
                    mPosX + mFixedCenterX - mCenterX,
                    mPosY + mFixedCenterY - mCenterY,
                    mButtonRadius,
                    mPaintCircleButton
            );
        }
    }


    /**
     * This is called during layout when the size of this view has changed.
     * Here we get the center of the view and the radius to draw all the shapes.
     *
     * @param w Current width of this view.
     * @param h Current height of this view.
     * @param oldW Old width of this view.
     * @param oldH Old height of this view.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        initPosition();

        // radius based on smallest size : height OR width
        int d = Math.min(w, h);
        mButtonRadius = (int) (d / 2 * mButtonSizeRatio);
        mBorderWidth  = (int) (w * mBackgroundSizeRatio);
        mBorderHeight = (int) (h * mBackgroundSizeRatio);
        mBackgroundWidth = mBorderWidth - (mPaintBorder.getStrokeWidth() / 2);
        mBackgroundHeight = mBorderHeight - (mPaintBorder.getStrokeWidth() / 2);

        if (mButtonBitmap != null)
            mButtonBitmap = Bitmap.createScaledBitmap(mButtonBitmap, mButtonRadius * 2, mButtonRadius * 2, true);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // setting the measured values to resize the view to a certain width and height
        int d = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec));
        setMeasuredDimension(d, d);
    }


    private int measure(int measureSpec) {
        if (MeasureSpec.getMode(measureSpec) == MeasureSpec.UNSPECIFIED) {
            // if no bounds are specified return a default size (200)
            return DEFAULT_SIZE;
        } else {
            // As you want to fill the available space
            // always return the full available bounds.
            return MeasureSpec.getSize(measureSpec);
        }
    }


    /*
    USER EVENT
     */


    /**
     * Handle touch screen motion event. Move the button according to the
     * finger coordinate and detect longPress by multiple pointers only.
     *
     * @param event The motion event.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // if disabled we don't move the
        if (!mEnabled) {
            return true;
        }


        // to move the button according to the finger coordinate
        // (or limited to one axe according to direction option)
        mPosY = mButtonDirection == 1 ? mCenterY : (int) event.getY();
        mPosX = mButtonDirection == 2 ? mCenterX : (int) event.getX();

        if (event.getAction() == MotionEvent.ACTION_UP) {

            if (mAlwaysCall == false && mThread != null) {
                // stop listener because the finger left the touch screen
                mThread.interrupt();
            }

            // re-center the button or not (depending on settings)
            maybeResetButtonPosition();

            // if mAutoReCenterButton is false we will send the last strength and angle a bit
            // later only after processing new position X and Y otherwise it could be above the border limit
            // if the user lifts their finger outside the border
            if (mAutoReCenterButtonX && mAutoReCenterButtonY) {
                if (mCallback != null)
                    mCallback.onMove(getDelX(), getDelY());
            }
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mCallback != null) {
                if (mAlwaysCall == false) {
                    if (mThread == null || mThread.isAlive() == false || mThread.isInterrupted()) {
                        mThread = new Thread(this);
                        mThread.start();
                    }
                }
                mCallback.onMove(getDelX(), getDelY());
            }
        }

        // handle first touch and long press with multiple touch only
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // when the first touch occurs we update the center (if set to auto-defined center)
                if (!mFixedCenter) {
                    mCenterX = mPosX;
                    mCenterY = mPosY;
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN: {
                // when the second finger touch
                if (event.getPointerCount() == 2) {
                    mHandlerMultipleLongPress.postDelayed(mRunnableMultipleLongPress, ViewConfiguration.getLongPressTimeout()*2);
                    mMoveTolerance = MOVE_TOLERANCE;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE:
                mMoveTolerance--;
                if (mMoveTolerance == 0) {
                    mHandlerMultipleLongPress.removeCallbacks(mRunnableMultipleLongPress);
                }
                break;

            case MotionEvent.ACTION_POINTER_UP: {
                // when the last multiple touch is released
                if (event.getPointerCount() == 2) {
                    mHandlerMultipleLongPress.removeCallbacks(mRunnableMultipleLongPress);
                }
                break;
            }
        }

        if (mPosX - mCenterX > mBorderWidth/2)
        {
            mPosX = mCenterX + mBorderWidth/2;
        }
        if (mCenterX - mPosX > mBorderWidth/2)
        {
            mPosX = mCenterX - mBorderWidth/2;
        }
        if (mPosY - mCenterY > mBorderHeight/2)
        {
            mPosY = mCenterY + mBorderHeight/2;
        }
        if (mCenterY - mPosY > mBorderHeight/2)
        {
            mPosY = mCenterY - mBorderHeight/2;
        }

        if (mAutoReCenterButtonX == false || mAutoReCenterButtonY == false) {
            // Now update the position if not reset to center
            if (mCallback != null)
                mCallback.onMove(getDelX(), getDelY());
        }


        // to force a new draw
        invalidate();

        return true;
    }


    /*
    GETTERS
     */

    private int getDelX() {
        return (int) (100 * (mPosX-mCenterX)/(mBorderWidth/2.0));
    }

    private int getDelY() {
        return (int) (100 * (mPosY-mCenterY)/(mBorderHeight/2.0));
    }


    /**
     * Reset the button position to the center if requested.
     */
    public void maybeResetButtonPosition() {
        if (mAutoReCenterButtonX)
            mPosX = mCenterX;
        if (mAutoReCenterButtonY)
            mPosY = mCenterY;
    }


    /**
     * Return the current direction allowed for the button to move
     * @return Actually return an integer corresponding to the direction:
     * - 1 means horizontal axis only,
     * - 2 means vertical axis only,
     * - 0 means both axes
     */
    public int getButtonDirection() {
        return mButtonDirection;
    }


    /**
     * Return the state of the joystick. False when the button don't move.
     * @return the state of the joystick
     */
    public boolean isEnabled() {
        return mEnabled;
    }


    /**
     * Return the size of the button (as a ratio of the total width/height)
     * Default is 0.25 (25%).
     * @return button size (value between 0.0 and 1.0)
     */
    public float getButtonSizeRatio() {
        return mButtonSizeRatio;
    }


    /**
     * Return the size of the background (as a ratio of the total width/height)
     * Default is 0.75 (75%).
     * @return background size (value between 0.0 and 1.0)
     */
    public float getmBackgroundSizeRatio() {
        return mBackgroundSizeRatio;
    }


    /**
     * Return the current behavior of the auto re-center button
     * @return True if automatically re-centered or False if not
     */
    public boolean isAutoReCenterButtonX() {
        return mAutoReCenterButtonX;
    }

    /**
     * Return the current behavior of the auto re-center button
     * @return True if automatically re-centered or False if not
     */
    public boolean isAutoReCenterButtonY() {
        return mAutoReCenterButtonY;
    }


    /**
     * Return the relative X coordinate of button center related
     * to top-left virtual corner of the border
     * @return coordinate of X (normalized between 0 and 100)
     */
    public int getNormalizedX() {
        if (getWidth() == 0) {
            return 50;
        }
        return Math.round((mPosX-mButtonRadius)*100.0f/(getWidth()-mButtonRadius*2));
    }


    /**
     * Return the relative Y coordinate of the button center related
     * to top-left virtual corner of the border
     * @return coordinate of Y (normalized between 0 and 100)
     */
    public int getNormalizedY() {
        if (getHeight() == 0) {
            return 50;
        }
        return Math.round((mPosY-mButtonRadius)*100.0f/(getHeight()-mButtonRadius*2));
    }


    /**
     * Return the alpha of the border
     * @return it should be an integer between 0 and 255 previously set
     */
    public int getBorderAlpha() {
        return mBorderAlpha;
    }

    /*
    SETTERS
     */


    /**
     * Set an image to the button with a drawable
     * @param d drawable to pick the image
     */
    public void setButtonDrawable(Drawable d) {
        if (d != null) {
            if (d instanceof BitmapDrawable) {
                mButtonBitmap = ((BitmapDrawable) d).getBitmap();

                if (mButtonRadius != 0) {
                    mButtonBitmap = Bitmap.createScaledBitmap(
                            mButtonBitmap,
                            mButtonRadius * 2,
                            mButtonRadius * 2,
                            true);
                }

                if (mPaintBitmapButton != null)
                    mPaintBitmapButton = new Paint();
            }
        }
    }


    /**
     * Set the button color for this JoystickView.
     * @param color the color of the button
     */
    public void setButtonColor(int color) {
        mPaintCircleButton.setColor(color);
        invalidate();
    }


    /**
     * Set the border color for this JoystickView.
     * @param color the color of the border
     */
    public void setBorderColor(int color) {
        mPaintBorder.setColor(color);
        if (color != Color.TRANSPARENT) {
            mPaintBorder.setAlpha(mBorderAlpha);
        }
        invalidate();
    }


    /**
     * Set the border alpha for this JoystickView.
     * @param alpha the transparency of the border between 0 and 255
     */
    public void setBorderAlpha(int alpha) {
        mBorderAlpha = alpha;
        mPaintBorder.setAlpha(alpha);
        invalidate();
    }


    /**
     * Set the background color for this JoystickView.
     * @param color the color of the background
     */
    @Override
    public void setBackgroundColor(int color) {
        mPaintBackground.setColor(color);
        invalidate();
    }


    /**
     * Set the border width for this JoystickView.
     * @param width the width of the border
     */
    public void setBorderWidth(int width) {
        mPaintBorder.setStrokeWidth(width);

        mBackgroundWidth = mBorderWidth - (width / 2);
        mBackgroundHeight = mBorderHeight - (width / 2);

        invalidate();
    }


    /**
     * Register a callback to be invoked when this JoystickView's button is moved
     * @param l The callback that will run
     */
    public void setOnMoveListener(OnMoveListener l) {
        setOnMoveListener(l, DEFAULT_LOOP_INTERVAL);
    }


    /**
     * Register a callback to be invoked when this JoystickView's button is moved
     * @param l The callback that will run
     * @param loopInterval Refresh rate to be invoked in milliseconds
     */
    public void setOnMoveListener(OnMoveListener l, int loopInterval) { setOnMoveListener(l, loopInterval, true); }

    /**
     * Register a callback to be invoked when this JoystickView's button is moved
     * @param l The callback that will run
     * @param loopInterval Refresh rate to be invoked in milliseconds
     * @param alwaysCall Whether the callback will be called even if the user is not touching the joystick
     */
    public void setOnMoveListener(OnMoveListener l, int loopInterval, boolean alwaysCall) {
        mCallback = l;
        mLoopInterval = loopInterval;
        mAlwaysCall = alwaysCall;

        if (mAlwaysCall && mCallback != null) {
            if (mThread != null)
                mThread.interrupt();

            mThread = new Thread(this);
            mThread.start();
        }
    }


    /**
     * Register a callback to be invoked when this JoystickView is touch and held by multiple pointers
     * @param l The callback that will run
     */
    public void setOnMultiLongPressListener(OnMultipleLongPressListener l) {
        mOnMultipleLongPressListener = l;
    }


    /**
     * Set the joystick center's behavior (fixed or auto-defined)
     * @param fixedCenter True for fixed center, False for auto-defined center based on touch down
     */
    public void setFixedCenter(boolean fixedCenter) {
        // if we set to "fixed" we make sure to re-init position related to the width of the joystick
        if (fixedCenter) {
            initPosition();
        }
        mFixedCenter = fixedCenter;
        invalidate();
    }


    /**
     * Enable or disable the joystick
     * @param enabled False mean the button won't move and onMove won't be called
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }


    /**
     * Set the joystick button size (as a fraction of the real width/height)
     * By default it is 25% (0.25).
     * @param newRatio between 0.0 and 1.0
     */
    public void setButtonSizeRatio(float newRatio) {
        if (newRatio > 0.0f & newRatio <= 1.0f) {
            mButtonSizeRatio = newRatio;
        }
    }


    /**
     * Set the joystick button size (as a fraction of the real width/height)
     * By default it is 75% (0.75).
     * Not working if the background is an image.
     * @param newRatio between 0.0 and 1.0
     */
    public void setBackgroundSizeRatio(float newRatio) {
        if (newRatio > 0.0f & newRatio <= 1.0f) {
            mBackgroundSizeRatio = newRatio;
        }
    }


    /**
     * Set the current behavior of the auto re-center button
     * @param b True if automatically re-centered or False if not
     */
    public void setAutoReCenterButtonX(boolean b) {
        mAutoReCenterButtonX = b;
    }

    /**
     * Set the current behavior of the auto re-center button
     * @param b True if automatically re-centered or False if not
     */
    public void setAutoReCenterButtonY(boolean b) {
        mAutoReCenterButtonY = b;
    }


    /**
     * Set the current allowed direction for the button to move
     */
    public void setButtonDirection(int direction) {
        mButtonDirection = direction;
    }


    /*
    IMPLEMENTS
     */


    @Override // Runnable
    public void run() {
        while (!Thread.interrupted()) {
            post(new Runnable() {
                public void run() {
                    if (mCallback != null)
                        mCallback.onMove(getDelX(), getDelY());
                }
            });

            try {
                Thread.sleep(mLoopInterval);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}