package main;

import java.util.ArrayList;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Vector2f;

import main.Ball.BallType;

public class PoolGame extends BasicGame {
	public static void main(String[] args) {
		try {
			PoolGame game = new PoolGame();
			AppGameContainer appgc = new AppGameContainer(game, PoolGame.WIDTH, PoolGame.HEIGHT, false);
			
			appgc.setShowFPS(false);
			appgc.setAlwaysRender(false);
			appgc.setForceExit(true);
			
			appgc.start();
		} catch (SlickException e) {
			e.printStackTrace();
		}
	}
	
	// public GUI constants
	public static final int WIDTH = 810;
	public static final int HEIGHT = 420;
	
	// private GUI constants
	private static final Color BCKG_COLOR = Color.lightGray;
	private static final Color BORDER_COLOR = Color.black;
	private static final Color POCKET_COLOR = new Color(0.2f, 0.2f, 0.2f, 0.2f);
	
	// private POCKETS constants
	private static final ImmutableVector2f[] POCKETS = new ImmutableVector2f[]{
			new ImmutableVector2f(0, 0),
			new ImmutableVector2f(405, 0),
			new ImmutableVector2f(810, 0),
			new ImmutableVector2f(0, 420),
			new ImmutableVector2f(405, 420),
			new ImmutableVector2f(810, 420)
	};
	private static final int POCKET_RADIUS = Ball.RADIUS * 2;
	
	// private CUE constants
	private static final float CUE_LENGTH = 200f;
	private static final Color CUE_COLOR = new Color(0, 0, 0, 0.5f);
	private static final Color PATH_COLOR = new Color(1f, 1f, 0, 0.5f);
	
	// public BALL constants
	private static final Vector2f racket = new Vector2f(600, 210);
	private static final Ball[] BALL_PRESETS = new Ball[]{
			new Ball(new Vector2f(200, 210), BallType.WHITE),
			
			new Ball(new Vector2f(Ball.RADIUS * -4, Ball.RADIUS * 0).add(racket), BallType.SOLIDS),
			
			new Ball(new Vector2f(Ball.RADIUS * -2, Ball.RADIUS * 1).add(racket), BallType.SOLIDS),
			new Ball(new Vector2f(Ball.RADIUS * -2, Ball.RADIUS * -1).add(racket), BallType.SOLIDS),
			
			new Ball(new Vector2f(Ball.RADIUS * 0, Ball.RADIUS * 0).add(racket), BallType.SOLIDS),
			new Ball(new Vector2f(Ball.RADIUS * 0, Ball.RADIUS * 2).add(racket), BallType.SOLIDS),
			new Ball(new Vector2f(Ball.RADIUS * 0, Ball.RADIUS * -2).add(racket), BallType.SOLIDS),

			new Ball(new Vector2f(Ball.RADIUS * 2, Ball.RADIUS * -3).add(racket), BallType.SOLIDS),
			new Ball(new Vector2f(Ball.RADIUS * 2, Ball.RADIUS * -1).add(racket), BallType.SOLIDS),
			new Ball(new Vector2f(Ball.RADIUS * 2, Ball.RADIUS * 1).add(racket), BallType.SOLIDS),
			new Ball(new Vector2f(Ball.RADIUS * 2, Ball.RADIUS * 3).add(racket), BallType.SOLIDS),
			
			new Ball(new Vector2f(Ball.RADIUS * 4, Ball.RADIUS * 0).add(racket), BallType.SOLIDS),
			new Ball(new Vector2f(Ball.RADIUS * 4, Ball.RADIUS * 2).add(racket), BallType.SOLIDS),
			new Ball(new Vector2f(Ball.RADIUS * 4, Ball.RADIUS * 4).add(racket), BallType.SOLIDS),
			new Ball(new Vector2f(Ball.RADIUS * 4, Ball.RADIUS * -2).add(racket), BallType.SOLIDS),
			new Ball(new Vector2f(Ball.RADIUS * 4, Ball.RADIUS * -4).add(racket), BallType.SOLIDS),
	};
	
	// private BALL variables
	private ArrayList<Ball> balls;
	
	// private CUE variables
	protected ImmutableVector2f cueDragNorm = new ImmutableVector2f(1, 0);
	private float cueDragLength = 0f;
	protected boolean cueReady = true;
	private float distToCollision = -1f;
	private boolean ballCollisionEstimated = false;
	private ArrayList<ImmutableVector2f> estCollisions = new ArrayList<ImmutableVector2f>();
	
	public PoolGame(String title) {
		super(title);
	}
	public PoolGame() {
		this("Eight-ball Pool Game");
	}
	
	@Override
	public void render(GameContainer container, Graphics g) throws SlickException {
		g.setLineWidth(2f);
		
		// draw background
		g.setAntiAlias(false);
		g.setColor(BORDER_COLOR);
		g.fillRect(0, 0, WIDTH, HEIGHT);
		
		// draw play area
		g.setColor(BCKG_COLOR);
		g.fillRect(2, 2, WIDTH - 4, HEIGHT - 4);
		
		// draw pockets
		g.setColor(POCKET_COLOR);
		for (ImmutableVector2f p : POCKETS) {
			g.fillOval(p.x - POCKET_RADIUS, p.y - POCKET_RADIUS, POCKET_RADIUS * 2, POCKET_RADIUS * 2);
		}
		
		// draw balls
		g.setAntiAlias(true);
		for (Ball b : balls) {
			if (b.pocketed)
				continue;
			
			g.setColor(b.color);
			g.fillOval(b.getPos().x - Ball.RADIUS, b.getPos().y - Ball.RADIUS, Ball.RADIUS * 2, Ball.RADIUS * 2);
		}
		
		if (cueReady) {
			// display cue
			ImmutableVector2f cueStart = balls.get(0).getPos().add(cueDragNorm.scale(Ball.RADIUS)).add(cueDragNorm.scale(cueDragLength));
			ImmutableVector2f cueEnd = cueStart.add(cueDragNorm.scale(CUE_LENGTH));
			g.setColor(CUE_COLOR);
			g.drawLine(cueStart.x, cueStart.y, cueEnd.x, cueEnd.y);
			
			if (distToCollision != -1) {
				// display estimated path
				ImmutableVector2f pathStart = balls.get(0).getPos().add(cueDragNorm.scale(-Ball.RADIUS));
				ImmutableVector2f pathEnd = pathStart.add(cueDragNorm.scale(-(distToCollision - Ball.RADIUS)));
				
				g.setColor(PATH_COLOR);
				g.drawLine(pathStart.x, pathStart.y, pathEnd.x, pathEnd.y);
				g.drawOval(pathEnd.x - Ball.RADIUS, pathEnd.y - Ball.RADIUS, Ball.RADIUS * 2, Ball.RADIUS * 2);
				
				if (ballCollisionEstimated) {
					// display estimated collision
					ImmutableVector2f whiteCollEnd = pathEnd.add(estCollisions.get(0));
					g.drawLine(pathEnd.x, pathEnd.y, whiteCollEnd.x, whiteCollEnd.y);
					
					ImmutableVector2f otherCollStart = pathEnd.add(estCollisions.get(1).normalise().scale(2 * Ball.RADIUS));
					ImmutableVector2f otherCollEnd = otherCollStart.add(estCollisions.get(1));
					g.drawLine(otherCollStart.x, otherCollStart.y, otherCollEnd.x, otherCollEnd.y);
				}
			}
		}
	}

	@Override
	public void init(GameContainer gc) throws SlickException {
		// add listeners
		gc.getInput().addKeyListener(new InputListener(this));
		gc.getInput().addMouseListener(new InputListener(this));
		
		// create balls
		balls = new ArrayList<Ball>();
		for (Ball b : BALL_PRESETS) {
			ImmutableVector2f spawn = new ImmutableVector2f(b.getPos());
			BallType t = b.type;
			
			balls.add(new Ball(spawn, t));
		}
	}

	@Override
	public void update(GameContainer container, int delta) throws SlickException {
		boolean allStopped = true;
		for (Ball b : balls) {
			b.tick(delta);
			allStopped &= b.isStopped();
		}
		cueReady = allStopped;
		if (cueReady) {
			if (getWhite().pocketed) {
				getWhite().respawn();
			}
		}
		
		for (Ball b : balls) {
			if (b.pocketed)
				continue;
			
			// check ball collisions
			for (Ball b2 : balls) {
				if (b.id >= b2.id)
					continue;
				
				ImmutableVector2f dist = b2.getPos().sub(b.getPos());
				float l = dist.length();
				
				if (l < Ball.RADIUS * 2) {
					Ball.ballCollision(b, b2, true);
				}
			}
			
			// check wall collisions
			if (b.getPos().x - Ball.RADIUS < 0) {
				// left
				b.collideBorder(new ImmutableVector2f(1, 0));
			}
			if (b.getPos().x + Ball.RADIUS > WIDTH) {
				// right
				b.collideBorder(new ImmutableVector2f(-1, 0));
			}
			if (b.getPos().y - Ball.RADIUS < 0) {
				// top
				b.collideBorder(new ImmutableVector2f(0, 1));
			}
			if (b.getPos().y + Ball.RADIUS > HEIGHT) {
				// bottom
				b.collideBorder(new ImmutableVector2f(0, -1));
			}
			
			// check pocketed
			for (ImmutableVector2f p : POCKETS) {
				if (b.getPos().sub(p).length() < POCKET_RADIUS) {
					b.pocket();
				}
			}
		}
		
		updateDistToCollision();
	}
	
	private void updateDistToCollision() {
		// calculate estimated path of white firing along -cueDragNorm
		ImmutableVector2f rayCentre = getWhitePos();
		
		for (float dist = 1; dist < Math.sqrt(WIDTH * WIDTH + HEIGHT * HEIGHT) + 500; dist += 1f) {
			distToCollision = -1;
			
			ImmutableVector2f distPos = rayCentre.add(cueDragNorm.scale(-dist));
			
			for (Ball b : balls) {
				if (b.type == BallType.WHITE)
					continue;
				
				ImmutableVector2f toBall = distPos.sub(b.getPos());
				
				if (toBall.length() <= 2 * Ball.RADIUS) {
					distToCollision = dist;
					
					Ball testBall = new Ball(distPos, BallType.WHITE);
					testBall.addForce(cueDragNorm.scale(-20f));
					estCollisions = Ball.ballCollision(testBall, b, false);
					
					ballCollisionEstimated = true;
					return;
				}
			}
			
			if (
					distPos.x - Ball.RADIUS < 0 ||
					distPos.x - Ball.RADIUS >= WIDTH ||
					distPos.x + Ball.RADIUS < 0 ||
					distPos.x + Ball.RADIUS >= WIDTH ||
					distPos.y - Ball.RADIUS < 0 ||
					distPos.y - Ball.RADIUS >= HEIGHT ||
					distPos.y + Ball.RADIUS < 0 ||
					distPos.y + Ball.RADIUS >= HEIGHT
			) {
				distToCollision = dist;
				ballCollisionEstimated = false;
				return;
			}
		}
	}
	public void shoot(Vector2f shot) {
		balls.get(0).addForce(shot);
		System.out.println("shot: " + shot.length());
		
		pullCue(0f);
	}
	public void shoot(ImmutableVector2f shot) {
		shoot(shot.makeVector2f());
	}
	
	public void pullCue(float dragDist) {
		cueDragLength = dragDist;
	}
	
	public void updateCue(int x, int y) {
		ImmutableVector2f toCursor = getWhitePos().sub(new Vector2f(x, y));
		cueDragNorm = toCursor.normalise();
	}
	
	public ImmutableVector2f getWhitePos() {
		return balls.get(0).getPos();
	}
	public Ball getWhite() {
		return balls.get(0);
	}
}
