package computing_mathematic;

import com.sun.opengl.util.j2d.TextRenderer;
import java.awt.Font;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

public class FPSmeter implements GLEventListener {
    
    private long t0;
    private long t1;
    public int FpS;
    private int frames;
    private TextRenderer textRenderer;
    
    public FPSmeter() {
    }
    public void init(GLAutoDrawable drawable) {
        textRenderer = new TextRenderer(new Font("Default", Font.PLAIN, 20));
    }
    public void display(GLAutoDrawable drawable) {
        frames++;
        t1 = System.currentTimeMillis();
        if (t1 - t0 >= 1000) {
            FpS = frames;
            t0 = t1;
            frames = 0;
        }
        
        textRenderer.beginRendering(drawable.getWidth(), drawable.getHeight());
        textRenderer.setColor(1f, 1f, 0f, 0.7f);
        textRenderer.draw(String.valueOf(FpS), 0, drawable.getHeight() - 20);
        textRenderer.endRendering();
    }
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }
    
}
