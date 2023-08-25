public class CustomDrawable extends Drawable {
    private Paint paint;
    private RectF rectF;
    private List<Integer> colours;
    private int activeSegment = -1;  // Default to no active segment
    private int numberOfSegments = 8;
    private int sweepAngle = 360 / numberOfSegments;

    public CustomDrawable() {
        paint = new Paint();
        paint.setAntiAlias(true);
        rectF = new RectF();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        rectF.set(bounds);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int startAngle = 0;

        setUpColours();

        for (int i = 0; i < numberOfSegments; i++) {
            // If this segment is the active segment, use a distinct color
            if (i == activeSegment) {
                paint.setColor(Color.RED);  // Change this to whatever highlight color you prefer
            } else {
                paint.setColor(colours.get(i % colours.size()));
            }
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);
            startAngle += sweepAngle;
        }
    }

    private void setUpColours() {
        colours = new ArrayList<>();
        // Add colors you want. This is just a basic example.
        colours.add(Color.GRAY);
        colours.add(Color.BLUE);
        colours.add(Color.GREEN);
        colours.add(Color.YELLOW);
        // Add more colors if needed.
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setActiveSegment(int segment) {
        this.activeSegment = segment;
        invalidateSelf();
    }
}
