
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedList;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 *
 * @author Hardik
 */

/*
 *  This class is the main class.
 */
public class RobotSimulator extends Canvas implements Runnable {

    private static final long serialVersionUID = 821929170520175588L;

    public static final int WIDTH = 1600, HEIGHT = WIDTH / 16 * 9; // Done to maintain aspect ratio of 16:9
    private Thread thread;
    private boolean running = false;
    public Rectangle2D angleSlider;
    public Cursor cursor;
    public double angleSliderX = Constants.ARM_SLIDER_CNTRL_X + 2, angleSliderY = Constants.ARM_SLIDER_CNTRL_Y + 2;
    public int angleSliderX1, angleSliderY1, angleSliderX2, angleSliderY2;
    public boolean isAngleSliderPickedUp = false;
    private Handler handler;
    private SpeedOMeter speedOMeter;
    public boolean isClawOpened = false;
    public State state = State.Simulation;
    private Random r;
    private int temperature;
    public boolean isImageVisible = false;
    private ImageRenderer imageRenderer;
    public Window window;

    public RobotSimulator() {
        handler = new Handler();

        r = new Random();
        temperature = r.nextInt(100);

        speedOMeter = new SpeedOMeter(handler);

        imageRenderer = new ImageRenderer(this);

        addKeyListener(new KeyInput(handler, this));
        addMouseListener(new MyMouseAdapter(this, handler));
        addMouseMotionListener(new MyMouseMotionListener(this, handler));

        handler.addObject(new Robot(Constants.ROBOT_CONT_X + Constants.ROBOT_CONT_WIDTH / 2 - Constants.ROBOT_WIDTH / 2, Constants.ROBOT_CONT_Y + Constants.ROBOT_CONT_HEIGHT / 2 - Constants.ROBOT_HEIGHT / 2, ObjectType.Robot));
        handler.addObject(new Arm(Constants.ARM_X, Constants.ARM_Y, ObjectType.Hand, this));
        window = new Window(WIDTH, HEIGHT, "Robot Simulator", this);
    }

    public static void main(String[] args) {
        new RobotSimulator();
    }

    public synchronized void start() {
        thread = new Thread(this);
        thread.start();
        running = true;
    }

    public synchronized void stop() {
        try {
            thread.join();
            running = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     The below code represents the loop which is used to show the changes that have been made to the states of the robot / arm.
     This will continuously call the render() function of this class which will draw the simulator UI with changes automatically without the need of any event.
     */
    @Override
    public void run() {

        this.requestFocus();
        long lastTime = System.nanoTime();
        double numberOfTicks = 60.0;
        double ns = 1000000000 / numberOfTicks;
        double delta = 0;
        long timer = System.currentTimeMillis();
        int frames = 0;
        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                tick();
                delta--;
            }
            if (running) {
                render();
            }
            frames++;
            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                frames = 0;
            }
        }
        stop();
    }

    /*
     This main function of this method is to change the state of the objects (robot/arm/speedometer) at each and evey tick.
     */
    public void tick() {
        if (state == State.Simulation) {
            handler.tick();
            speedOMeter.tick();
        }

    }

    /*
     The method actually handles all the drawing of the simulator UI including all its components.
     */
    public void render() {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            this.createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();

        g.setColor(Color.BLACK); //setting the background color for the app
        g.fillRect(0, 0, WIDTH, HEIGHT); //filling the backgroung color

        Font largeBoldFont = new Font("arial", 1, 30);
        Font mediumNormalFont = new Font("arial", 0, 20);
        Font mediumBoldFont = new Font("arial", 1, 20);
        Font smallNormalFont = new Font("arial", 0, 18);

        /*
         Depending upon the state of the application draw the appropriate UI
         */
        if (state == State.Simulation) {
            g.setColor(Color.WHITE);

            g.setFont(largeBoldFont);
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(Constants.HELP_BTN_X - 178, Constants.HELP_BTN_Y, Constants.HELP_BTN_WIDTH + 178, Constants.HELP_BTN_HEIGHT); //rectangle for the help button
            g.setColor(Color.red);
            g.drawString("HELP & KEYBOARD", Constants.HELP_BTN_X - 158, Constants.HELP_BTN_Y + 34);// text for the help button
            g.setColor(Color.WHITE);

            g.drawRect(Constants.ROBOT_CONT_X, Constants.ROBOT_CONT_Y, Constants.ROBOT_CONT_WIDTH, Constants.ROBOT_CONT_HEIGHT); //drawing the rectangle for the robot to move about

            g.setFont(mediumNormalFont);
            g.drawString("Robot Simulation:", Constants.ROBOT_CONT_X + 5, Constants.ROBOT_CONT_Y - 12);// text for the robot simulation box

            g.drawRect(Constants.ARM_CONT_X, Constants.ARM_CONT_Y, Constants.ARM_CONT_WIDTH, Constants.ARM_CONT_HEIGHT); // rectangle for showing the arm simulation
            g.drawString("Arm Simulation:", Constants.ARM_CONT_X + 5, Constants.ARM_CONT_Y - 12); //text for arm simulation

            g.setFont(smallNormalFont);
            g.drawRect(Constants.ROBOT_CNTRL_X, Constants.ROBOT_CNTRL_Y, Constants.ROBOT_CNTRL_WIDTH, Constants.ROBOT_CNTRL_HEIGHT); //drawing rectangle for the robot controls
            g.drawString("Robot Controls:", Constants.ROBOT_CNTRL_X + 5, Constants.ROBOT_CNTRL_Y - 12);

            //up arrow for robot control
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(Constants.UP_CNTRL_BOX_X, Constants.UP_CNTRL_BOX_Y, Constants.UP_CNTRL_BOX_WIDTH, Constants.UP_CNTRL_BOX_HEIGHT);
            double phi = Math.PI / 6;
            Graphics2D g2 = (Graphics2D) g;
            int size = 20;
            int x0 = Constants.UP_CNTRL_BOX_X + Constants.UP_CNTRL_BOX_WIDTH / 2;
            int y0 = Constants.UP_CNTRL_BOX_Y + Constants.UP_CNTRL_BOX_HEIGHT / 3;
            double x = size * Math.cos(40);
            double y = size * Math.sin(40);
            g.setColor(Color.red);
            g2.setStroke(new BasicStroke(4));
            g2.draw(new Line2D.Double(x0, y0, x0 + x, y0 + y));
            g2.draw(new Line2D.Double(x0, y0, x0 - x, y0 + y));

            //right arrow for robot control
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(Constants.RIGHT_CNTRL_BOX_X, Constants.RIGHT_CNTRL_BOX_Y, Constants.RIGHT_CNTRL_BOX_WIDTH, Constants.RIGHT_CNTRL_BOX_HEIGHT);
            x0 = Constants.RIGHT_CNTRL_BOX_X + Constants.RIGHT_CNTRL_BOX_WIDTH - Constants.RIGHT_CNTRL_BOX_WIDTH / 3;
            y0 = Constants.RIGHT_CNTRL_BOX_Y + Constants.RIGHT_CNTRL_BOX_HEIGHT / 2;
            x = -1 * size * Math.cos(40);
            y = -1 * size * Math.sin(40);
            g.setColor(Color.red);
            g2.draw(new Line2D.Double(x0, y0, x0 - x, y0 + y));
            g2.draw(new Line2D.Double(x0, y0, x0 - x, y0 - y));

            //down arrow for robot control
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(Constants.DOWN_CNTRL_BOX_X, Constants.DOWN_CNTRL_BOX_Y, Constants.DOWN_CNTRL_BOX_WIDTH, Constants.DOWN_CNTRL_BOX_HEIGHT);
            x0 = Constants.DOWN_CNTRL_BOX_X + Constants.DOWN_CNTRL_BOX_WIDTH / 2;
            y0 = Constants.DOWN_CNTRL_BOX_Y + Constants.DOWN_CNTRL_BOX_HEIGHT - Constants.DOWN_CNTRL_BOX_HEIGHT / 3;
            x = size * Math.cos(40);
            y = size * Math.sin(40);
            g.setColor(Color.red);
            g2.draw(new Line2D.Double(x0, y0, x0 + x, y0 - y));
            g2.draw(new Line2D.Double(x0, y0, x0 - x, y0 - y));

            //left arow for robot control
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(Constants.LEFT_CNTRL_BOX_X, Constants.LEFT_CNTRL_BOX_Y, Constants.LEFT_CNTRL_BOX_WIDTH, Constants.LEFT_CNTRL_BOX_HEIGHT);
            x0 = Constants.LEFT_CNTRL_BOX_X + Constants.LEFT_CNTRL_BOX_WIDTH / 3;
            y0 = Constants.LEFT_CNTRL_BOX_Y + Constants.LEFT_CNTRL_BOX_HEIGHT / 2;
            x = size * Math.cos(40);
            y = size * Math.sin(40);
            g.setColor(Color.red);
            g2.draw(new Line2D.Double(x0, y0, x0 - x, y0 + y));
            g2.draw(new Line2D.Double(x0, y0, x0 - x, y0 - y));

            g2.setStroke(new BasicStroke());
            g.setColor(Color.WHITE);

            //Arm controller
            g.drawRect(Constants.ARM_SLIDER_CONT_X, Constants.ARM_SLIDER_CONT_Y, Constants.ARM_SLIDER_CONT_WIDTH, Constants.ARM_SLIDER_CONT_HEIGHT);
            g.drawString("Arm Angle Control:", Constants.ARM_SLIDER_CONT_X, Constants.ARM_SLIDER_CONT_Y - 12);
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(Constants.ARM_SLIDER_MINUS_X, Constants.ARM_SLIDER_MINUS_Y, Constants.ARM_SLIDER_MINUS_WIDTH, Constants.ARM_SLIDER_MINUS_HEIGHT);
            g.fillRect(Constants.ARM_SLIDER_PLUS_X, Constants.ARM_SLIDER_PLUS_Y, Constants.ARM_SLIDER_PLUS_WIDTH, Constants.ARM_SLIDER_PLUS_HEIGHT);
            g.setColor(Color.WHITE);
            g.drawLine(Constants.ARM_SLIDER_CNTRL_X + 2, Constants.ARM_SLIDER_CNTRL_Y + 2 + (Constants.ARM_SLIDER_CNTRL_HEIGHT / 2), Constants.ARM_SLIDER_CNTRL_X + Constants.ARM_SLIDER_CNTRL_WIDTH - 2, Constants.ARM_SLIDER_CNTRL_Y + 2 + (Constants.ARM_SLIDER_CNTRL_HEIGHT / 2));
            angleSlider = new Rectangle2D.Double(angleSliderX, angleSliderY, Constants.ARM_SLIDER_WIDTH, Constants.ARM_SLIDER_HEIGHT);
            g2.fill(angleSlider);
            g.setColor(Color.red);
            Font icon = new Font("arial", 1, 50);
            g.setFont(icon);
            g.drawString("-", Constants.ARM_SLIDER_MINUS_X + Constants.ARM_SLIDER_MINUS_WIDTH / 2 - 10, Constants.ARM_SLIDER_MINUS_Y + Constants.ARM_SLIDER_MINUS_HEIGHT / 2 + 12);
            g.drawString("+", Constants.ARM_SLIDER_PLUS_X + Constants.ARM_SLIDER_PLUS_WIDTH / 2 - 14, Constants.ARM_SLIDER_PLUS_Y + Constants.ARM_SLIDER_PLUS_HEIGHT / 2 + 17);
            g.setColor(Color.WHITE);

            //Open close claw control
            g.setFont(smallNormalFont);
            g.drawString("Claw Control:", Constants.ARM_SLIDER_CONT_X, Constants.ARM_SLIDER_CONT_Y + Constants.ARM_SLIDER_CONT_HEIGHT + 70);
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(Constants.CLAW_BUTTON_X, Constants.CLAW_BUTTON_Y, Constants.CLAW_BUTTON_WIDTH, Constants.CLAW_BUTTON_HEIGHT);
            g.setFont(mediumBoldFont);
            g.setColor(Color.red);
            g.drawString("OPEN / CLOSE", Constants.ARM_SLIDER_CONT_X + 150, Constants.ARM_SLIDER_CONT_Y + Constants.ARM_SLIDER_CONT_HEIGHT + 72);

            //Temperature Reading Box
            g.setFont(smallNormalFont);
            g.setColor(Color.WHITE);
            g.drawString("Temperature Sensor:", Constants.TEMP_BOX_X, Constants.TEMP_BOX_Y - 12);
            g.setColor(Color.WHITE);
            g.drawRect(Constants.TEMP_BOX_X, Constants.TEMP_BOX_Y, Constants.TEMP_BOX_WIDTH, Constants.TEMP_BOX_HEIGHT);
            g.drawString("Reading: " + temperature + "C", Constants.TEMP_BOX_X + 30, Constants.TEMP_BOX_Y + Constants.TEMP_BOX_HEIGHT / 2 + 5);
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(Constants.TEMP_REFRESH_BUTTON_X, Constants.TEMP_REFRESH_BUTTON_Y, Constants.TEMP_REFRESH_BUTTON_WIDTH, Constants.TEMP_REFRESH_BUTTON_HEIGHT);
            g.setFont(mediumBoldFont);
            g.setColor(Color.red);
            g.drawString("REFRESH", Constants.TEMP_REFRESH_BUTTON_X + 25, Constants.TEMP_REFRESH_BUTTON_Y + 35);

            //Camera control
            g.setColor(Color.WHITE);
            g.drawRect(Constants.CAMERA_CONTROL_X, Constants.CAMERA_CONTROL_Y, Constants.CAMERA_CONTROL_WIDTH, Constants.CAMERA_CONTROL_HEIGHT);
            g.setFont(smallNormalFont);
            g.drawString("Camera Feed:", Constants.CAMERA_CONTROL_X, Constants.CAMERA_CONTROL_Y - 12);
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(Constants.CAMERA_CAPTURE_BTN_X, Constants.CAMERA_CAPTURE_BTN_Y, Constants.CAMERA_CAPTURE_BTN_WIDTH, Constants.CAMERA_CAPTURE_BTN_HEIGHT);
            g.setColor(Color.red);
            g.setFont(mediumBoldFont);
            g.drawString("CAPTURE", Constants.CAMERA_CAPTURE_BTN_X + 25, Constants.CAMERA_CAPTURE_BTN_Y + 30);

            Font smallerNormalFont = new Font("arial", 0, 18);
            g.setColor(Color.WHITE);
            g.setFont(smallerNormalFont);
            g.drawString("(The robot will start rotating once left/right key is pressed.", Constants.TEMP_BOX_X - 70, Constants.TEMP_BOX_Y + Constants.TEMP_BOX_HEIGHT + 25);
            g.drawString("It will not stop until any one of the keys is pressed again)", Constants.TEMP_BOX_X - 70, Constants.TEMP_BOX_Y + Constants.TEMP_BOX_HEIGHT + 50);

            handler.render(g);

            speedOMeter.render(g);

            imageRenderer.render(g);
        } else if (state == State.Help) {
            int x = 20;
            int y = Constants.HELP_BTN_Y + Constants.HELP_BTN_HEIGHT + 20;

            g.setColor(Color.WHITE);

            g.setFont(largeBoldFont);
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(Constants.HELP_BTN_X, Constants.HELP_BTN_Y, Constants.HELP_BTN_WIDTH, Constants.HELP_BTN_HEIGHT); //rectangle for the help button
            g.setColor(Color.red);
            g.drawString("BACK", Constants.HELP_BTN_X + 28, Constants.HELP_BTN_Y + 34);// text for the help button
            g.setColor(Color.WHITE);

            g.drawString("Robot Controls:", x, y - 35);
            g.setFont(mediumNormalFont);
            g.drawString("You can use the on-screen motion control buttons or keyboard shortcuts to control the movement of the robot. The robot will move in any directions.", x, y);
            g.drawString("W = Accelerate       S = Deaccelerate / Go reverse", x, y + 30);
            g.drawString("A = Start rotating left", x, y + 60);
            g.drawString("D = Start rotating right", x, y + 90);
            g.drawString("The robot will keep rotating unless told to stop. To stop rotating just press left/right.", x, y + 120);

            g.setFont(largeBoldFont);
            g.drawString("Arm Control:", x, y + 200);
            g.setFont(mediumNormalFont);
            g.drawString("The arm can be rotated from 0-90 degrees by dragging the on-screen control or by pressing the +/- buttons. It can also be controlled from the keyboard as:", x, y + 230);
            g.drawString("R = Rotate up        F = Rotate Down", x, y + 260);

            g.setFont(largeBoldFont);
            g.drawString("Claw Control:", x, y + 340);
            g.setFont(mediumNormalFont);
            g.drawString("The claw can be opened and closed any time from the on-screen controls or can be done via the keyboard as:", x, y + 370);
            g.drawString("Q = Toggle claw status (Open / Close)", x, y + 400);

            g.setFont(largeBoldFont);
            g.drawString("Camera Feed Control:", x, y + 480);
            g.setFont(mediumNormalFont);
            g.drawString("The camera on the robot can be asked to click a photo of the environment. The robot will capture the image and show it on the screen.", x, y + 510);
            g.drawString("The captured image can be resized and can be viewed separately just by clicking on it.", x, y + 540);
            g.drawString("The keyboard button for capturing an image is C.", x, y + 570);

            g.setFont(largeBoldFont);
            g.drawString("Temperature Sensor Control:", x, y + 650);
            g.setFont(mediumNormalFont);
            g.drawString("The robot has a sensor for recording the temperature of the environment it is in.", x, y + 680);
            g.drawString("You can refresh the reading on the screen using the on-screen button or the keyboard shortcut", x, y + 710);
            g.drawString("T = Refresh temperature reading", x, y + 740);
        }

        g.dispose();
        bs.show();
    }

    /*
     This method is used to set the slider position according to the angle change caued due to either button click or keyboard action.
     */
    public void setAngleSliderPosition(double angle) {
        double x = (angle * 2) + (Constants.ARM_SLIDER_CNTRL_X + 2);
        angleSliderX = x;
        angleSliderY = Constants.ARM_SLIDER_CNTRL_Y + 2;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }
}


/*
 This enum has the different states the application can be in.
 Simulation - the UI shows the simulation of the robot
 Help - the UI shows the help screen
 */
enum State {

    Simulation, Help;
}

/*
 This class represents the window onto which our robot simulator is shown
 */
class Window extends Canvas {

    public Window(int width, int height, String title, RobotSimulator simulator) {
        JFrame frame = new JFrame(title);

        frame.setPreferredSize(new Dimension(width, height));
        frame.setMaximumSize(new Dimension(width, height));
        frame.setMinimumSize(new Dimension(width, height));

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.add(simulator);
        frame.setVisible(true);
        frame.setFocusable(true);
        frame.requestFocus();
        simulator.start();
    }
}

/*
 This class will act as a base class for all the objects that will be shown on the simulator screen.
 */
abstract class HWLayerObject {

    protected double x, y;
    protected ObjectType id;
    protected double angle;

    /*
     This method will help in changing the state of the object on each and every tick i.e. change of the screen so as to maintain a constant change of state of the object.
     */
    public abstract void tick();

    /*
     This method is implemented to render the object onto the screen
     */
    public abstract void render(Graphics g);

    public HWLayerObject(double x, double y, ObjectType id) {
        this.x = x;
        this.y = y;
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public ObjectType getId() {
        return id;
    }

    public void setId(ObjectType id) {
        this.id = id;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

}

/*
 This enum represents the type of objects that are present on the screen
 */
enum ObjectType {

    Robot, Hand;
}


/*
 This class represents the Robot object on the screen
 */
class Robot extends HWLayerObject {

    private double velX, velY;
    private double speed;
    private double validAngle;
    private double validX, validY;
    private Rectangle2D rectangle;
    private boolean isTurning = false;
    private Direction direction;

    public enum Direction {

        Right, Left;
    }

    public Robot(double x, double y, ObjectType id) {
        super(x, y, id);
        this.angle = 0;
        this.velX = 0;
        this.velY = 0;
        this.speed = 0;
        this.validAngle = 0;
        this.validX = 0;
        this.validY = 0;
    }

    public Rectangle2D getRectangle() {
        return rectangle;
    }

    public void setRectangle(Rectangle2D rectangle) {
        this.rectangle = rectangle;
    }

    public boolean isIsTurning() {
        return isTurning;
    }

    public void setIsTurning(boolean isTurning) {
        this.isTurning = isTurning;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public double getVelX() {
        return velX;
    }

    public void setVelX(double velX) {
        this.velX = velX;
    }

    public double getVelY() {
        return velY;
    }

    public void setVelY(double velY) {
        this.velY = velY;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    @Override
    public void tick() {
        velX = speed * Math.sin(Math.toRadians(angle));
        velY = -1 * speed * Math.cos(Math.toRadians(angle));
        x += velX;
        y += velY;
        if (isTurning) {
            if (direction == Direction.Left) {
                angle -= Constants.ANGLE_STEP;
            } else {
                angle += Constants.ANGLE_STEP;
            }
        }
    }

    @Override
    public void render(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.green);
        rectangle = new Rectangle2D.Double(x, y, Constants.ROBOT_WIDTH, Constants.ROBOT_HEIGHT);
        Ellipse2D.Double circle1 = new Ellipse2D.Double(rectangle.getX() + 2, rectangle.getY() + 5, 12, 12);
        Ellipse2D.Double circle2 = new Ellipse2D.Double(rectangle.getX() + rectangle.getWidth() - 15, rectangle.getY() + 5, 12, 12);
        Rectangle2D.Double arm = new Rectangle2D.Double(rectangle.getX() + rectangle.getWidth() / 2 - 4, rectangle.getY() + 2, 8, rectangle.getHeight() - 10);

        AffineTransform transform = new AffineTransform();
        transform.rotate(Math.toRadians(angle), rectangle.getX() + rectangle.getWidth() / 2, rectangle.getY() + rectangle.getHeight() / 2);

        Shape transformedRectangle = transform.createTransformedShape(rectangle);
        Shape transformedCircle1 = transform.createTransformedShape(circle1);
        Shape transformedCircle2 = transform.createTransformedShape(circle2);
        Shape transformedArm = transform.createTransformedShape(arm);

        if (checkIfRobotisInBounds(transformedRectangle)) {
            g2d.fill(transformedRectangle);
            g2d.setColor(Color.GRAY);
            g2d.fill(transformedCircle1);
            g2d.fill(transformedCircle2);
            g2d.setColor(Color.red);
            g2d.fill(transformedArm);
            validAngle = angle;
            validX = x;
            validY = y;
        } else {
            x = validX;
            y = validY;
            angle = validAngle;
            rectangle = new Rectangle2D.Double(validX, validY, Constants.ROBOT_WIDTH, Constants.ROBOT_HEIGHT);
            Ellipse2D.Double circle11 = new Ellipse2D.Double(rectangle.getX() + 2, rectangle.getY() + 5, 12, 12);
            Ellipse2D.Double circle12 = new Ellipse2D.Double(rectangle.getX() + rectangle.getWidth() - 15, rectangle.getY() + 5, 12, 12);
            Rectangle2D.Double arm1 = new Rectangle2D.Double(rectangle.getX() + rectangle.getWidth() / 2 - 4, rectangle.getY() + 2, 8, rectangle.getHeight() - 10);
            AffineTransform transform1 = new AffineTransform();
            transform1.rotate(Math.toRadians(validAngle), rectangle.getX() + rectangle.getWidth() / 2, rectangle.getY() + rectangle.getHeight() / 2);
            Shape transformedRectangle1 = transform1.createTransformedShape(rectangle);
            Shape transformedCircle11 = transform1.createTransformedShape(circle11);
            Shape transformedCircle12 = transform1.createTransformedShape(circle12);
            Shape transformedArm1 = transform1.createTransformedShape(arm1);
            if (checkIfRobotisInBounds(transformedRectangle1)) {
                g2d.setColor(Color.green);
                g2d.fill(transformedRectangle1);
                g2d.setColor(Color.GRAY);
                g2d.fill(transformedCircle11);
                g2d.fill(transformedCircle12);
                g2d.setColor(Color.red);
                g2d.fill(transformedArm1);
                validAngle = angle;
            }
        }
    }

    /*
     This method will check if the robot is within the bounds.
     It will return true if the robot is within bounds else it will return false.
     */
    private boolean checkIfRobotisInBounds(Shape transformedRobot) {
        double maxX = transformedRobot.getBounds2D().getMaxX() + 2;
        double minX = transformedRobot.getBounds2D().getMinX() - 2;
        double maxY = transformedRobot.getBounds2D().getMaxY() + 2;
        double minY = transformedRobot.getBounds2D().getMinY() - 2;
        if (minX >= Constants.ROBOT_CONT_X && maxX <= Constants.ROBOT_CONT_X + Constants.ROBOT_CONT_WIDTH && minY >= Constants.ROBOT_CONT_Y && maxY <= Constants.ROBOT_CONT_Y + Constants.ROBOT_CONT_HEIGHT) {
            return true;
        }
        return false;
    }
}


/*
 This class repesents the Arm object in the simulator
 */
class Arm extends HWLayerObject {

    RobotSimulator simulator;

    public Arm(int x, int y, ObjectType id, RobotSimulator simulator) {
        super(x, y, id);
        this.angle = 0;
        this.simulator = simulator;
    }

    @Override
    public void tick() {

    }

    @Override
    public void render(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.red);
        Shape arm = new Rectangle2D.Double(x, y, Constants.ARM_WIDTH, Constants.ARM_HEIGHT);
        Shape leftClaw = new Line2D.Double(Constants.ARM_X + Constants.ARM_WIDTH, Constants.ARM_Y + Constants.ARM_HEIGHT / 2, Constants.ARM_X + Constants.ARM_WIDTH + Constants.CLAW_LENGTH, Constants.ARM_Y + Constants.ARM_HEIGHT / 2);
        Shape rightClaw = new Line2D.Double(Constants.ARM_X + Constants.ARM_WIDTH, Constants.ARM_Y + Constants.ARM_HEIGHT / 2, Constants.ARM_X + Constants.ARM_WIDTH + Constants.CLAW_LENGTH, Constants.ARM_Y + Constants.ARM_HEIGHT / 2);

        if (simulator.isClawOpened) {
            AffineTransform leftClawTransform = new AffineTransform();
            AffineTransform rightClawTransform = new AffineTransform();

            leftClawTransform.rotate(Math.toRadians(-45), Constants.ARM_X + Constants.ARM_WIDTH, Constants.ARM_Y + Constants.ARM_HEIGHT / 2);
            leftClaw = leftClawTransform.createTransformedShape(leftClaw);
            rightClawTransform.rotate(Math.toRadians(45), Constants.ARM_X + Constants.ARM_WIDTH, Constants.ARM_Y + Constants.ARM_HEIGHT / 2);
            rightClaw = rightClawTransform.createTransformedShape(rightClaw);
        }

        AffineTransform armTransform = new AffineTransform();
        armTransform.rotate(Math.toRadians(-angle), Constants.ARM_X, Constants.ARM_Y + Constants.ARM_HEIGHT);
        leftClaw = armTransform.createTransformedShape(leftClaw);
        rightClaw = armTransform.createTransformedShape(rightClaw);
        arm = armTransform.createTransformedShape(arm);

        g2d.draw(leftClaw);
        g2d.draw(rightClaw);

        g2d.fill(arm);
    }

}

/*
 This class will handle all the objects that are present on the screen.
 */
class Handler {

    LinkedList<HWLayerObject> object = new LinkedList<HWLayerObject>();

    public void tick() {
        for (int i = 0; i < object.size(); i++) {
            HWLayerObject tempObject = object.get(i);
            tempObject.tick();
        }
    }

    public void render(Graphics g) {
        for (int i = 0; i < object.size(); i++) {
            HWLayerObject tempObject = object.get(i);
            tempObject.render(g);
        }
    }

    public void addObject(HWLayerObject object) {
        this.object.add(object);
    }

    public void removeObject(HWLayerObject object) {
        this.object.remove(object);
    }
}

/*
 This class handles the display of the Speedometer on the screen
 */
class SpeedOMeter {

    private int stepSize = Constants.SPEED_BAR_HEIGHT / (Constants.SPEED_STEP_COUNT + 1);
    private Handler handler;

    public SpeedOMeter(Handler handler) {
        this.handler = handler;
    }

    public void tick() {

    }

    public void render(Graphics g) {
        g.setColor(Color.white);
        for (int i = 1; i <= Constants.SPEED_STEP_COUNT + 1; i++) {
            g.drawRect(Constants.SPEED_BAR_X, Constants.SPEED_BAR_Y, Constants.SPEED_BAR_WIDTH, stepSize * i);
        }
        g.drawRect(Constants.SPEED_BAR_X, Constants.SPEED_BAR_Y, Constants.SPEED_BAR_WIDTH, Constants.SPEED_BAR_HEIGHT);
        for (int i = 0; i < handler.object.size(); i++) {
            HWLayerObject tempObject = handler.object.get(i);
            if (tempObject.getId() == ObjectType.Robot) {
                if (((Robot) tempObject).getSpeed() < 0) {
                    g.setColor(Color.red);
                    g.fillRect(Constants.SPEED_BAR_X + 1, Constants.SPEED_BAR_Y + (stepSize * 3) + 1, Constants.SPEED_BAR_WIDTH - 1, stepSize - 1);
                } else {
                    int count = ((int) (((Robot) tempObject).getSpeed())) / Constants.SPEED_STEP;
                    if (count == Constants.SPEED_STEP_COUNT) {
                        g.setColor(Color.red);
                    } else if (count == Constants.SPEED_STEP_COUNT - 1) {
                        g.setColor(new Color(20, 155, 45));
                    } else {
                        g.setColor(Color.green);
                    }

                    for (int j = count; j > 0; j--) {
                        g.fillRect(Constants.SPEED_BAR_X + 1, Constants.SPEED_BAR_Y + ((Constants.SPEED_STEP_COUNT - j) * stepSize) + 1, Constants.SPEED_BAR_WIDTH - 1, stepSize - 1);
                    }
                }

                Font font = new Font("arial", 0, 12);
                g.setFont(font);
                g.setColor(Color.WHITE);
                g.drawString("SPEED", Constants.SPEED_BAR_X, Constants.SPEED_BAR_Y - 10);
                g.drawString("MAX", Constants.SPEED_BAR_X + Constants.SPEED_BAR_WIDTH + 3, Constants.SPEED_BAR_Y + 5);
                g.drawString("MED", Constants.SPEED_BAR_X + Constants.SPEED_BAR_WIDTH + 3, Constants.SPEED_BAR_Y + stepSize + 5);
                g.drawString("LOW", Constants.SPEED_BAR_X + Constants.SPEED_BAR_WIDTH + 3, Constants.SPEED_BAR_Y + (stepSize * 2) + 5);
                g.drawString("REV", Constants.SPEED_BAR_X + Constants.SPEED_BAR_WIDTH + 3, Constants.SPEED_BAR_Y + (stepSize * 4) + 5);
            }
        }
    }
}

/*
 This class will show the pop up of the captured image to the user. The pop up is resizable.
 */
class CapturedImage extends JComponent {

    private static final long serialVersionUID = 738501432349633567L;
    BufferedImage image;

    public CapturedImage() {
        try {
            String currentDirectory = System.getProperty("user.dir");
            File inputFile = new File(currentDirectory + "/" + Constants.FILE_NAME);
            image = ImageIO.read(inputFile);
        } catch (Exception e) {
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (image == null) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null);
        g2d.dispose();
    }

}


/*
 This class will get the image from the file and display it onto the screen
 */
class ImageRenderer {

    public ImageRenderer(RobotSimulator simulator) {
        this.simulator = simulator;
    }
    RobotSimulator simulator;

    public void render(Graphics g) {
        if (simulator.isImageVisible) {
            try {
                String currentDirectory = System.getProperty("user.dir");
                File inputFile = new File(currentDirectory + "/" + Constants.FILE_NAME);
                BufferedImage image = ImageIO.read(inputFile);
                if (image == null) {
                    return;
                }
                Graphics2D g2d = (Graphics2D) g;
                g2d.drawImage(image, Constants.IMAGE_X, Constants.IMAGE_Y, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT, null);
            } catch (Exception e) {
            }
        }
    }
}

/*
 This class will handle all the mouse events that are required in the simulator
 */
class MyMouseMotionListener extends MouseMotionAdapter {

    RobotSimulator simulator;
    Handler handler;

    public MyMouseMotionListener(RobotSimulator simulator, Handler handler) {
        super();
        this.simulator = simulator;
        this.handler = handler;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (simulator.state == State.Simulation) {                                                  // if the state of the application is simulation
            if (simulator.angleSlider.contains(e.getX(), e.getY())) {                               //checking if the slider for the arm is dragged
                if (simulator.isAngleSliderPickedUp) {
                    if ((int) simulator.angleSlider.getX() > Constants.ARM_SLIDER_CNTRL_X
                            && (int) simulator.angleSlider.getX() + Constants.ARM_SLIDER_WIDTH < Constants.ARM_SLIDER_CNTRL_X + Constants.ARM_SLIDER_CNTRL_WIDTH
                            && (int) simulator.angleSliderY > Constants.ARM_SLIDER_CNTRL_Y
                            && (int) simulator.angleSliderY + Constants.ARM_SLIDER_HEIGHT < Constants.ARM_SLIDER_CNTRL_Y + Constants.ARM_SLIDER_CNTRL_HEIGHT) {

                        simulator.angleSliderX2 = e.getX();
                        simulator.angleSliderY2 = e.getY();
                        simulator.angleSliderX = simulator.angleSliderX + simulator.angleSliderX2 - simulator.angleSliderX1;
                        simulator.angleSliderY = simulator.angleSliderY + simulator.angleSliderY2 - simulator.angleSliderY1;
                        if (simulator.angleSliderX <= Constants.ARM_SLIDER_CNTRL_X) {
                            simulator.angleSliderX = Constants.ARM_SLIDER_CNTRL_X + 1;
                        }
                        if (simulator.angleSliderX + Constants.ARM_SLIDER_WIDTH >= Constants.ARM_SLIDER_CNTRL_X + Constants.ARM_SLIDER_CNTRL_WIDTH) {
                            simulator.angleSliderX = simulator.angleSliderX - (simulator.angleSliderX + Constants.ARM_SLIDER_WIDTH - Constants.ARM_SLIDER_CNTRL_X - Constants.ARM_SLIDER_CNTRL_WIDTH) - 1;
                        }

                        if (simulator.angleSliderY <= Constants.ARM_SLIDER_CNTRL_Y) {
                            simulator.angleSliderY = Constants.ARM_SLIDER_CNTRL_Y + 1;
                        }
                        if (simulator.angleSliderY + Constants.ARM_SLIDER_HEIGHT >= Constants.ARM_SLIDER_CNTRL_Y + Constants.ARM_SLIDER_CNTRL_HEIGHT) {
                            simulator.angleSliderY = simulator.angleSliderY - (simulator.angleSliderY + Constants.ARM_SLIDER_HEIGHT - Constants.ARM_SLIDER_CNTRL_Y - Constants.ARM_SLIDER_CNTRL_HEIGHT) - 1;
                        }
                        simulator.angleSliderX1 = simulator.angleSliderX2;
                        simulator.angleSliderY1 = simulator.angleSliderY2;

                        for (int i = 0; i < handler.object.size(); i++) {
                            HWLayerObject tempObject = handler.object.get(i);
                            if (tempObject.getId() == ObjectType.Hand) {
                                double angle = (simulator.angleSlider.getX() - (Constants.ARM_SLIDER_CNTRL_X + 2)) / 2;
                                if (angle >= 0 && angle <= 90) {
                                    tempObject.setAngle((simulator.angleSlider.getX() - (Constants.ARM_SLIDER_CNTRL_X + 2)) / 2);
                                }
                            }
                        }
                    }

                }
            }
        }

    }

}


/*
 This class will handle all the mouse click events for the application.
 */
class MyMouseAdapter extends MouseAdapter {

    RobotSimulator simulator;
    Handler handler;

    public MyMouseAdapter(RobotSimulator simulator, Handler handler) {
        super();
        this.simulator = simulator;
        this.handler = handler;
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        if (simulator.state == State.Simulation) {
            if (simulator.angleSlider.contains(e.getX(), e.getY())) {
                simulator.isAngleSliderPickedUp = false;
            }
        }

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (simulator.state == State.Simulation) {
            if (simulator.angleSlider.contains(e.getX(), e.getY())) {
                simulator.isAngleSliderPickedUp = true;
            }
            simulator.angleSliderX1 = e.getX();
            simulator.angleSliderY1 = e.getY();
        }

    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();
        if (simulator.state == State.Simulation) {
            if (mouseOver(mx, my, Constants.CLAW_BUTTON_X, Constants.CLAW_BUTTON_Y, Constants.CLAW_BUTTON_WIDTH, Constants.CLAW_BUTTON_HEIGHT)) {           // if the claw control button is clicked
                simulator.isClawOpened = !simulator.isClawOpened;
            } else if (mouseOver(mx, my, Constants.ARM_SLIDER_MINUS_X, Constants.ARM_SLIDER_MINUS_Y, Constants.ARM_SLIDER_MINUS_WIDTH, Constants.ARM_SLIDER_MINUS_HEIGHT)) { //if the arm slider minus is clicked
                for (int i = 0; i < handler.object.size(); i++) {
                    HWLayerObject tempObject = handler.object.get(i);
                    if (tempObject.getId() == ObjectType.Hand) {
                        if (tempObject.getAngle() != 0) {
                            tempObject.setAngle(tempObject.getAngle() - Constants.ANGLE_STEP);
                            simulator.setAngleSliderPosition(tempObject.getAngle() - Constants.ANGLE_STEP);
                        }
                    }
                }
            } else if (mouseOver(mx, my, Constants.ARM_SLIDER_PLUS_X, Constants.ARM_SLIDER_PLUS_Y, Constants.ARM_SLIDER_PLUS_WIDTH, Constants.ARM_SLIDER_PLUS_HEIGHT)) { // if the arm slider plus is clicked
                for (int i = 0; i < handler.object.size(); i++) {
                    HWLayerObject tempObject = handler.object.get(i);
                    if (tempObject.getId() == ObjectType.Hand) {
                        if (tempObject.getAngle() != 90) {
                            tempObject.setAngle(tempObject.getAngle() + Constants.ANGLE_STEP);
                            simulator.setAngleSliderPosition(tempObject.getAngle() + Constants.ANGLE_STEP);
                        }
                    }
                }
            } else if (mouseOver(mx, my, Constants.UP_CNTRL_BOX_X, Constants.UP_CNTRL_BOX_Y, Constants.UP_CNTRL_BOX_WIDTH, Constants.UP_CNTRL_BOX_HEIGHT)) {    //if the UP in robot control is clicked
                for (int i = 0; i < handler.object.size(); i++) {
                    HWLayerObject tempObject = handler.object.get(i);
                    if (tempObject.getId() == ObjectType.Robot) {
                        if (((Robot) tempObject).getSpeed() == Constants.SPEED_MAX_LIMIT) {
                            return;
                        }
                        ((Robot) tempObject).setSpeed(((Robot) tempObject).getSpeed() + Constants.SPEED_STEP);
                    }
                }

            } else if (mouseOver(mx, my, Constants.LEFT_CNTRL_BOX_X, Constants.LEFT_CNTRL_BOX_Y, Constants.LEFT_CNTRL_BOX_WIDTH, Constants.LEFT_CNTRL_BOX_HEIGHT)) {    //if the LEFT in robot control is clicked
                for (int i = 0; i < handler.object.size(); i++) {
                    HWLayerObject tempObject = handler.object.get(i);
                    if (tempObject.getId() == ObjectType.Robot) {
                        if (((Robot) tempObject).isIsTurning()) {
                            ((Robot) tempObject).setIsTurning(false);
                            return;
                        }
                        ((Robot) tempObject).setIsTurning(true);
                        ((Robot) tempObject).setDirection(Robot.Direction.Left);
                        ((Robot) tempObject).setAngle(((Robot) tempObject).getAngle() - Constants.ANGLE_STEP);
                    }
                }
            } else if (mouseOver(mx, my, Constants.DOWN_CNTRL_BOX_X, Constants.DOWN_CNTRL_BOX_Y, Constants.DOWN_CNTRL_BOX_WIDTH, Constants.DOWN_CNTRL_BOX_HEIGHT)) {    //if the DOWN in robot control is clicked
                for (int i = 0; i < handler.object.size(); i++) {
                    HWLayerObject tempObject = handler.object.get(i);
                    if (tempObject.getId() == ObjectType.Robot) {
                        if (((Robot) tempObject).getSpeed() == Constants.SPEED_REVERSE) {
                            return;
                        }
                        ((Robot) tempObject).setSpeed(((Robot) tempObject).getSpeed() - Constants.SPEED_STEP);
                    }
                }
            } else if (mouseOver(mx, my, Constants.RIGHT_CNTRL_BOX_X, Constants.RIGHT_CNTRL_BOX_Y, Constants.RIGHT_CNTRL_BOX_WIDTH, Constants.RIGHT_CNTRL_BOX_HEIGHT)) {    //if the RIGHT in robot control is clicked
                for (int i = 0; i < handler.object.size(); i++) {
                    HWLayerObject tempObject = handler.object.get(i);
                    if (tempObject.getId() == ObjectType.Robot) {
                        if (((Robot) tempObject).isIsTurning()) {
                            ((Robot) tempObject).setIsTurning(false);
                            return;
                        }
                        ((Robot) tempObject).setIsTurning(true);
                        ((Robot) tempObject).setDirection(Robot.Direction.Right);
                        ((Robot) tempObject).setAngle(((Robot) tempObject).getAngle() + Constants.ANGLE_STEP);
                    }
                }
            } else if (mouseOver(mx, my, Constants.TEMP_REFRESH_BUTTON_X, Constants.TEMP_REFRESH_BUTTON_Y, Constants.TEMP_REFRESH_BUTTON_WIDTH, Constants.TEMP_REFRESH_BUTTON_HEIGHT)) {    //if temperature refresh is clicked
                simulator.setTemperature(new Random().nextInt(100));
            } else if (mouseOver(mx, my, Constants.CAMERA_CAPTURE_BTN_X, Constants.CAMERA_CAPTURE_BTN_Y, Constants.CAMERA_CAPTURE_BTN_WIDTH, Constants.CAMERA_CAPTURE_BTN_HEIGHT)) {    //if camera capture is clicked
                simulator.isImageVisible = true;
            } else if (mouseOver(mx, my, Constants.IMAGE_X, Constants.IMAGE_Y, Constants.IMAGE_WIDTH, Constants.IMAGE_HEIGHT)) {    //if the captured image is clicked
                if (simulator.isImageVisible) {
                    JFrame imageWindow = new JFrame();
                    imageWindow.setTitle("Captured Image");
                    imageWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    imageWindow.setBounds(simulator.window.getX() + 100, simulator.window.getY() + 100, 600, 600);
                    imageWindow.getContentPane().add(new CapturedImage());
                    imageWindow.setVisible(true);
                }
            } else if (mouseOver(mx, my, Constants.HELP_BTN_X - 178, Constants.HELP_BTN_Y, Constants.HELP_BTN_WIDTH + 178, Constants.HELP_BTN_HEIGHT)) {        //if the help button is clicked
                for (int i = 0; i < handler.object.size(); i++) {
                    HWLayerObject tempObject = handler.object.get(i);
                    if (tempObject.getId() == ObjectType.Robot) {
                        ((Robot) tempObject).setAngle(0);
                        ((Robot) tempObject).setIsTurning(false);
                        ((Robot) tempObject).setSpeed(0);
                    }
                }
                simulator.state = State.Help;
            }
        } else if (simulator.state == State.Help) {
            if (mouseOver(mx, my, Constants.HELP_BTN_X, Constants.HELP_BTN_Y, Constants.HELP_BTN_WIDTH, Constants.HELP_BTN_HEIGHT)) {    //if the back button is pressed
                simulator.state = State.Simulation;
            }
        }
    }

    /*
     This method is used to check if the mouse is over a certain button with given x,y,width and height. 
     It will return true if the mouse is over the given area else it will return false.
     */
    private boolean mouseOver(int mx, int my, int x, int y, int width, int height) {
        if (mx > x && mx < x + width) {
            if (my > y && my < y + height) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}

/*
 This class is used to handle the keyboard inputs for our application
 */
class KeyInput extends KeyAdapter {

    private Handler handler;
    private RobotSimulator simulator;
    private boolean[] keyDown = new boolean[4];

    public KeyInput(Handler handler, RobotSimulator simulator) {
        this.handler = handler;
        this.simulator = simulator;
        for (int i = 0; i < keyDown.length; i++) {
            keyDown[i] = false;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getExtendedKeyCode();
        if (simulator.state == State.Simulation) {
            for (int i = 0; i < handler.object.size(); i++) {
                HWLayerObject tempObject = handler.object.get(i);

                if (tempObject.getId() == ObjectType.Robot) {
                    if (key == KeyEvent.VK_W) {
                        keyDown[0] = false;
                    } else if (key == KeyEvent.VK_A) {
                        keyDown[3] = false;
                    } else if (key == KeyEvent.VK_S) {
                        keyDown[1] = false;
                    } else if (key == KeyEvent.VK_D) {
                        keyDown[2] = false;
                    }

                    if (keyDown[0]) {
                        if (((Robot) tempObject).getSpeed() == Constants.SPEED_MAX_LIMIT) {
                            return;
                        }
                        ((Robot) tempObject).setSpeed(((Robot) tempObject).getSpeed() + Constants.SPEED_STEP);
                    }
                    if (keyDown[1]) {
                        if (((Robot) tempObject).getSpeed() == Constants.SPEED_REVERSE) {
                            return;
                        }
                        ((Robot) tempObject).setSpeed(((Robot) tempObject).getSpeed() - Constants.SPEED_STEP);
                    }
                    if (keyDown[2]) {
                        if (((Robot) tempObject).isIsTurning()) {
                            ((Robot) tempObject).setIsTurning(false);
                            return;
                        }
                        ((Robot) tempObject).setIsTurning(true);
                        ((Robot) tempObject).setDirection(Robot.Direction.Right);
                        ((Robot) tempObject).setAngle(((Robot) tempObject).getAngle() + Constants.ANGLE_STEP);
                    }
                    if (keyDown[3]) {
                        if (((Robot) tempObject).isIsTurning()) {
                            ((Robot) tempObject).setIsTurning(false);
                            return;
                        }
                        ((Robot) tempObject).setIsTurning(true);
                        ((Robot) tempObject).setDirection(Robot.Direction.Left);
                        ((Robot) tempObject).setAngle(((Robot) tempObject).getAngle() - Constants.ANGLE_STEP);
                    }

                } else if (tempObject.getId() == ObjectType.Hand) {
                    if (key == KeyEvent.VK_R) {
                        if (tempObject.getAngle() != 90) {
                            tempObject.setAngle(tempObject.getAngle() + Constants.ANGLE_STEP);
                            simulator.setAngleSliderPosition(tempObject.getAngle() + Constants.ANGLE_STEP);
                        }
                    } else if (key == KeyEvent.VK_F) {
                        if (tempObject.getAngle() != 0) {
                            tempObject.setAngle(tempObject.getAngle() - Constants.ANGLE_STEP);
                            simulator.setAngleSliderPosition(tempObject.getAngle() - Constants.ANGLE_STEP);
                        }
                    }
                }
            }
            if (key == KeyEvent.VK_T) {
                simulator.setTemperature(new Random().nextInt(100));
            } else if (key == KeyEvent.VK_C) {
                simulator.isImageVisible = true;
            } else if (key == KeyEvent.VK_Q) {
                simulator.isClawOpened = !simulator.isClawOpened;
            }
        }

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (simulator.state == State.Simulation) {
            int key = e.getExtendedKeyCode();
            for (int i = 0; i < handler.object.size(); i++) {
                HWLayerObject tempObject = handler.object.get(i);

                if (tempObject.getId() == ObjectType.Robot) {
                    if (key == KeyEvent.VK_W) {
                        if (((Robot) tempObject).getSpeed() == Constants.SPEED_MAX_LIMIT) {
                            return;
                        }
                        ((Robot) tempObject).setSpeed(((Robot) tempObject).getSpeed() + Constants.SPEED_STEP);
                        keyDown[0] = true;
                    } else if (key == KeyEvent.VK_A) {
                        if (((Robot) tempObject).isIsTurning()) {
                            ((Robot) tempObject).setIsTurning(false);
                            return;
                        }
                        ((Robot) tempObject).setIsTurning(true);
                        ((Robot) tempObject).setDirection(Robot.Direction.Left);
                        ((Robot) tempObject).setAngle(((Robot) tempObject).getAngle() - Constants.ANGLE_STEP);
                        keyDown[3] = true;
                    } else if (key == KeyEvent.VK_S) {
                        if (((Robot) tempObject).getSpeed() == Constants.SPEED_REVERSE) {
                            return;
                        }
                        ((Robot) tempObject).setSpeed(((Robot) tempObject).getSpeed() - Constants.SPEED_STEP);
                        keyDown[1] = true;
                    } else if (key == KeyEvent.VK_D) {
                        if (((Robot) tempObject).isIsTurning()) {
                            ((Robot) tempObject).setIsTurning(false);
                            return;
                        }
                        ((Robot) tempObject).setIsTurning(true);
                        ((Robot) tempObject).setDirection(Robot.Direction.Right);
                        ((Robot) tempObject).setAngle(((Robot) tempObject).getAngle() + Constants.ANGLE_STEP);
                        keyDown[2] = true;
                    }
                } else if (tempObject.getId() == ObjectType.Hand) {
                    if (key == KeyEvent.VK_R) {
                        if (tempObject.getAngle() != 90) {
                            tempObject.setAngle(tempObject.getAngle() + Constants.ANGLE_STEP);
                            simulator.setAngleSliderPosition(tempObject.getAngle() + Constants.ANGLE_STEP);
                        }
                    } else if (key == KeyEvent.VK_F) {
                        if (tempObject.getAngle() != 0) {
                            tempObject.setAngle(tempObject.getAngle() - Constants.ANGLE_STEP);
                            simulator.setAngleSliderPosition(tempObject.getAngle() - Constants.ANGLE_STEP);
                        }
                    }
                }
            }

        }
    }
}

/*
 This class contains all the constants that are required throughout the application
 */
class Constants {

    public static final int ROBOT_CONT_X = 5;
    public static final int ROBOT_CONT_Y = 40;
    public static final int ROBOT_CONT_WIDTH = 650;
    public static final int ROBOT_CONT_HEIGHT = RobotSimulator.HEIGHT - 85;

    public static final int ARM_CONT_X = ROBOT_CONT_X + ROBOT_CONT_WIDTH + 20;
    public static final int ARM_CONT_Y = 40;
    public static final int ARM_CONT_WIDTH = 330;
    public static final int ARM_CONT_HEIGHT = 330;

    public static final int HELP_BTN_X = RobotSimulator.WIDTH - Constants.HELP_BTN_WIDTH - 12;
    public static final int HELP_BTN_Y = 10;
    public static final int HELP_BTN_WIDTH = 150;
    public static final int HELP_BTN_HEIGHT = 50;

    public static final int ROBOT_CNTRL_X = ROBOT_CONT_X + ROBOT_CONT_WIDTH + 80;
    public static final int ROBOT_CNTRL_Y = ROBOT_CONT_Y + ROBOT_CONT_HEIGHT - 220;
    public static final int ROBOT_CNTRL_WIDTH = 200;
    public static final int ROBOT_CNTRL_HEIGHT = 200;

    public static final int ROBOT_CNTRL_ARROW_BOX_SIZE = 50;

    public static final int UP_CNTRL_BOX_X = ROBOT_CNTRL_X + (ROBOT_CNTRL_WIDTH / 2) - (ROBOT_CNTRL_ARROW_BOX_SIZE / 2);
    public static final int UP_CNTRL_BOX_Y = ROBOT_CNTRL_Y + (ROBOT_CNTRL_ARROW_BOX_SIZE / 4);
    public static final int UP_CNTRL_BOX_WIDTH = ROBOT_CNTRL_ARROW_BOX_SIZE;
    public static final int UP_CNTRL_BOX_HEIGHT = ROBOT_CNTRL_ARROW_BOX_SIZE;

    public static final int RIGHT_CNTRL_BOX_X = ROBOT_CNTRL_X + ROBOT_CNTRL_WIDTH - ROBOT_CNTRL_ARROW_BOX_SIZE - (ROBOT_CNTRL_ARROW_BOX_SIZE / 4);
    public static final int RIGHT_CNTRL_BOX_Y = ROBOT_CNTRL_Y + (ROBOT_CNTRL_HEIGHT / 2) - (ROBOT_CNTRL_ARROW_BOX_SIZE / 2);
    public static final int RIGHT_CNTRL_BOX_WIDTH = ROBOT_CNTRL_ARROW_BOX_SIZE;
    public static final int RIGHT_CNTRL_BOX_HEIGHT = ROBOT_CNTRL_ARROW_BOX_SIZE;

    public static final int DOWN_CNTRL_BOX_X = ROBOT_CNTRL_X + (ROBOT_CNTRL_WIDTH / 2) - (ROBOT_CNTRL_ARROW_BOX_SIZE / 2);
    public static final int DOWN_CNTRL_BOX_Y = ROBOT_CNTRL_Y + ROBOT_CNTRL_HEIGHT - ROBOT_CNTRL_ARROW_BOX_SIZE - (ROBOT_CNTRL_ARROW_BOX_SIZE / 4);
    public static final int DOWN_CNTRL_BOX_WIDTH = ROBOT_CNTRL_ARROW_BOX_SIZE;
    public static final int DOWN_CNTRL_BOX_HEIGHT = ROBOT_CNTRL_ARROW_BOX_SIZE;

    public static final int LEFT_CNTRL_BOX_X = ROBOT_CNTRL_X + (ROBOT_CNTRL_ARROW_BOX_SIZE / 4);
    public static final int LEFT_CNTRL_BOX_Y = ROBOT_CNTRL_Y + (ROBOT_CNTRL_HEIGHT / 2) - (ROBOT_CNTRL_ARROW_BOX_SIZE / 2);
    public static final int LEFT_CNTRL_BOX_WIDTH = ROBOT_CNTRL_ARROW_BOX_SIZE;
    public static final int LEFT_CNTRL_BOX_HEIGHT = ROBOT_CNTRL_ARROW_BOX_SIZE;

    public static final int ARM_SLIDER_CONT_X = ARM_CONT_X;
    public static final int ARM_SLIDER_CONT_Y = ARM_CONT_Y + ARM_CONT_HEIGHT + 50;
    public static final int ARM_SLIDER_CONT_WIDTH = ARM_CONT_WIDTH - 15;
    public static final int ARM_SLIDER_CONT_HEIGHT = 60;

    public static final int ARM_SLIDER_CONTROLS_SIZE = 40;

    public static final int ARM_SLIDER_MINUS_X = ARM_SLIDER_CONT_X + 5;
    public static final int ARM_SLIDER_MINUS_Y = ARM_SLIDER_CONT_Y + 10;
    public static final int ARM_SLIDER_MINUS_WIDTH = ARM_SLIDER_CONTROLS_SIZE;
    public static final int ARM_SLIDER_MINUS_HEIGHT = ARM_SLIDER_CONTROLS_SIZE;

    public static final int ARM_SLIDER_CNTRL_X = ARM_SLIDER_MINUS_X + ARM_SLIDER_MINUS_WIDTH + 10;
    public static final int ARM_SLIDER_CNTRL_Y = ARM_SLIDER_CONT_Y + 8;
    public static final int ARM_SLIDER_CNTRL_WIDTH = 203;
    public static final int ARM_SLIDER_CNTRL_HEIGHT = ARM_SLIDER_CONTROLS_SIZE + 4;

    public static final int ARM_SLIDER_WIDTH = 20;
    public static final int ARM_SLIDER_HEIGHT = ARM_SLIDER_CONTROLS_SIZE;

    public static final int ARM_SLIDER_PLUS_X = ARM_SLIDER_MINUS_X + ARM_SLIDER_MINUS_WIDTH + 8 + ARM_SLIDER_CNTRL_WIDTH + 10;
    public static final int ARM_SLIDER_PLUS_Y = ARM_SLIDER_CONT_Y + 10;
    public static final int ARM_SLIDER_PLUS_WIDTH = ARM_SLIDER_CONTROLS_SIZE;
    public static final int ARM_SLIDER_PLUS_HEIGHT = ARM_SLIDER_CONTROLS_SIZE;

    public static final int SPEED_STEP = 1;
    public static final int SPEED_STEP_COUNT = 3;
    public static final int SPEED_MAX_LIMIT = SPEED_STEP * SPEED_STEP_COUNT;
    public static final int SPEED_REVERSE = -SPEED_STEP;

    public static final int ROBOT_WIDTH = 50;
    public static final int ROBOT_HEIGHT = 50;

    public static final double ANGLE_STEP = 0.5;

    public static final int SPEED_BAR_WIDTH = 40;
    public static final int SPEED_BAR_HEIGHT = 100;
    public static final int SPEED_BAR_X = ROBOT_CONT_X + ROBOT_CONT_WIDTH - SPEED_BAR_WIDTH - 50;
    public static final int SPEED_BAR_Y = ROBOT_CONT_Y + 30;

    public static final int CLAW_LENGTH = 30;

    public static final int ARM_WIDTH = ARM_CONT_WIDTH - CLAW_LENGTH - 40;
    public static final int ARM_HEIGHT = 5;
    public static final int ARM_X = ARM_CONT_X + (int) (CLAW_LENGTH * Math.sin(Math.toRadians(45))) + 8;
    public static final int ARM_Y = ARM_CONT_Y + ARM_CONT_HEIGHT - (int) (CLAW_LENGTH * Math.sin(Math.toRadians(45))) - 10;

    public static final int CLAW_BUTTON_X = Constants.ARM_SLIDER_CONT_X + 120;
    public static final int CLAW_BUTTON_Y = Constants.ARM_SLIDER_CONT_Y + Constants.ARM_SLIDER_CONT_HEIGHT + 40;
    public static final int CLAW_BUTTON_WIDTH = 200;
    public static final int CLAW_BUTTON_HEIGHT = 50;

    public static final int TEMP_BOX_X = ARM_CONT_X + ARM_CONT_WIDTH + 20;
    public static final int TEMP_BOX_Y = ROBOT_CNTRL_Y + 60;
    public static final int TEMP_BOX_WIDTH = 540;
    public static final int TEMP_BOX_HEIGHT = 100;

    public static final int TEMP_REFRESH_BUTTON_WIDTH = 150;
    public static final int TEMP_REFRESH_BUTTON_HEIGHT = 60;
    public static final int TEMP_REFRESH_BUTTON_X = TEMP_BOX_X + TEMP_BOX_WIDTH - TEMP_REFRESH_BUTTON_WIDTH - 50;
    public static final int TEMP_REFRESH_BUTTON_Y = TEMP_BOX_Y + TEMP_BOX_HEIGHT - TEMP_REFRESH_BUTTON_HEIGHT - 20;

    public static final String FILE_NAME = "hpt150030.png";

    public static final int CAMERA_CONTROL_X = ARM_CONT_X + ARM_CONT_WIDTH + 20;
    public static final int CAMERA_CONTROL_Y = HELP_BTN_Y + HELP_BTN_HEIGHT + 80;
    public static final int CAMERA_CONTROL_WIDTH = 540;
    public static final int CAMERA_CONTROL_HEIGHT = 500;

    public static final int CAMERA_CAPTURE_BTN_WIDTH = 150;
    public static final int CAMERA_CAPTURE_BTN_HEIGHT = 50;
    public static final int CAMERA_CAPTURE_BTN_X = CAMERA_CONTROL_X + CAMERA_CONTROL_WIDTH - CAMERA_CAPTURE_BTN_WIDTH - 20;
    public static final int CAMERA_CAPTURE_BTN_Y = CAMERA_CONTROL_Y + 20;

    public static final int IMAGE_X = CAMERA_CONTROL_X + 45;
    public static final int IMAGE_Y = CAMERA_CAPTURE_BTN_Y + CAMERA_CAPTURE_BTN_HEIGHT + 50;
    public static final int IMAGE_WIDTH = 450;
    public static final int IMAGE_HEIGHT = 350;
}
