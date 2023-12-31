package invaders.engine;

import java.util.ArrayList;
import java.util.List;

import invaders.ConfigReader;
import invaders.builder.BunkerBuilder;
import invaders.builder.Director;
import invaders.builder.EnemyBuilder;
import invaders.factory.PlayerProjectile;
import invaders.factory.Projectile;
import invaders.gameobject.Bunker;
import invaders.gameobject.Enemy;
import invaders.gameobject.GameObject;
import invaders.entities.Player;
import invaders.mementoUndo.Caretaker;
import invaders.mementoUndo.GameMemento;
import invaders.observer.ConcreteScoreObs;
import invaders.observer.Observer;
import invaders.observer.Subject;
import invaders.rendering.Renderable;
import org.json.simple.JSONObject;

/**
 * This class manages the main loop and logic of the game
 */
public class GameEngine implements Subject {

	private static GameEngine instance = null;
	private static final String EASY_CONFIG = "src/main/resources/config_easy.json";
	private static final String MEDIUM_CONFIG = "src/main/resources/config_medium.json";
	private static final String HARD_CONFIG = "src/main/resources/config_hard.json";
	private List<GameObject> gameObjects; // A list of game objects that gets updated each frame
	private List<Renderable> renderables;

	private List<GameObject> pendingToAddGameObject;
	private List<GameObject> pendingToRemoveGameObject;

	private List<Renderable> pendingToAddRenderable;
	private List<Renderable> pendingToRemoveRenderable;

	private ConcreteScoreObs scoreObserver ;
	private Player player;

	private boolean left;
	private boolean right;
	private int gameWidth;
	private int gameHeight;
	private int timer = 45;


	private GameEngine(){
		initialize(EASY_CONFIG);
	}
	public static GameEngine getInstance(){
		if(instance == null){
			synchronized (GameEngine.class) {
				if (instance == null) {
					instance = new GameEngine();
				}
			}
		}
		return instance;
	}
	public void changeDifficultyLevel(int levelCode){
		if(levelCode == 1){
			initialize(EASY_CONFIG);
		}else if(levelCode == 2){
			initialize(MEDIUM_CONFIG);
		}else if (levelCode == 3) {
			initialize(HARD_CONFIG);
		}
		return;
	}

	private void initialize(String config){
		// Read the config here
		renderables = new ArrayList<>();
		gameObjects = new ArrayList<>();
		pendingToRemoveGameObject = new ArrayList<>();
		pendingToAddGameObject = new ArrayList<>();
		pendingToAddRenderable = new ArrayList<>();
		pendingToRemoveRenderable = new ArrayList<>();
		ConfigReader.parse(config);
		this.scoreObserver = new ConcreteScoreObs(0);
		// Get game width and height
		gameWidth = ((Long)((JSONObject) ConfigReader.getGameInfo().get("size")).get("x")).intValue();
		gameHeight = ((Long)((JSONObject) ConfigReader.getGameInfo().get("size")).get("y")).intValue();

		//Get player info
		this.player = new Player(ConfigReader.getPlayerInfo());
		renderables.add(player);


		Director director = new Director();
		BunkerBuilder bunkerBuilder = new BunkerBuilder();
		//Get Bunkers info
		for(Object eachBunkerInfo:ConfigReader.getBunkersInfo()){
			Bunker bunker = director.constructBunker(bunkerBuilder, (JSONObject) eachBunkerInfo);
			gameObjects.add(bunker);
			renderables.add(bunker);
		}


		EnemyBuilder enemyBuilder = new EnemyBuilder();
		//Get Enemy info
		for(Object eachEnemyInfo:ConfigReader.getEnemiesInfo()){
			Enemy enemy = director.constructEnemy(this,enemyBuilder,(JSONObject)eachEnemyInfo);
			gameObjects.add(enemy);
			renderables.add(enemy);
		}

	}



	/**
	 * Updates the game/simulation
	 */
	public void update(){
		timer+=1;

		movePlayer();

		for(GameObject go: gameObjects){
			go.update(this);
		}

		for (int i = 0; i < renderables.size(); i++) {
			Renderable renderableA = renderables.get(i);
			for (int j = i+1; j < renderables.size(); j++) {
				Renderable renderableB = renderables.get(j);



				if((renderableA.getRenderableObjectName().equals("Enemy") && renderableB.getRenderableObjectName().equals("EnemyProjectile"))
						||(renderableA.getRenderableObjectName().equals("EnemyProjectile") && renderableB.getRenderableObjectName().equals("Enemy"))||
						(renderableA.getRenderableObjectName().equals("EnemyProjectile") && renderableB.getRenderableObjectName().equals("EnemyProjectile"))){
					//If the game still running and not finish
					//System.out.println("abc " + timer/60);

				}else{
					//notifyObserver();
					if(renderableA.isColliding(renderableB) && (renderableA.getHealth()>0 && renderableB.getHealth()>0)) {

						if(renderableA instanceof PlayerProjectile) {
							this.scoreObserver.checking(renderableB);
							notifyObserver();
						}else if(renderableB instanceof PlayerProjectile){
							this.scoreObserver.checking(renderableA);
							notifyObserver();
						}

						renderableA.takeDamage(1);
						renderableB.takeDamage(1);

						//System.out.println("hit something");
					}
				}
			}

		}


		// ensure that renderable foreground objects don't go off-screen
		int offset = 1;
		for(Renderable ro: renderables){
			if(!ro.getLayer().equals(Renderable.Layer.FOREGROUND)){
				continue;
			}
			if(ro.getPosition().getX() + ro.getWidth() >= gameWidth) {
				ro.getPosition().setX((gameWidth - offset) -ro.getWidth());
			}

			if(ro.getPosition().getX() <= 0) {
				ro.getPosition().setX(offset);
			}

			if(ro.getPosition().getY() + ro.getHeight() >= gameHeight) {
				ro.getPosition().setY((gameHeight - offset) -ro.getHeight());
			}

			if(ro.getPosition().getY() <= 0) {
				ro.getPosition().setY(offset);
			}
		}

	}
	public boolean checkIfGameNotEnd(){
		boolean playerAlive;
		boolean enemyStillThere = false;
		for(Renderable renderable: this.renderables){
			if(renderable.getRenderableObjectName().equals("Enemy") && renderable.isAlive()){
				enemyStillThere = true;
				break;
			}
		}
		playerAlive = player.isAlive();
		return playerAlive && enemyStillThere;
	}
	public List<Renderable> getRenderables(){
		return renderables;
	}
	public void setRenderables(List<Renderable> renderables){this.renderables = renderables;}
	public void setGameObjects(List<GameObject> gameObjects) {this.gameObjects = gameObjects;}

	public List<GameObject> getGameObjects() {
		return gameObjects;
	}
	public List<GameObject> getPendingToAddGameObject() {
		return pendingToAddGameObject;
	}

	public List<GameObject> getPendingToRemoveGameObject() {
		return pendingToRemoveGameObject;
	}

	public List<Renderable> getPendingToAddRenderable() {
		return pendingToAddRenderable;
	}

	public List<Renderable> getPendingToRemoveRenderable() {
		return pendingToRemoveRenderable;
	}


	public void leftReleased() {
		this.left = false;
	}

	public void rightReleased(){
		this.right = false;
	}

	public void leftPressed() {
		this.left = true;
	}
	public void rightPressed(){
		this.right = true;
	}

	public boolean shootPressed(){

		if(timer>45 && player.isAlive()){
			Projectile projectile = player.shoot();
			gameObjects.add(projectile);
			renderables.add(projectile);
			timer=0;
			return true;
		}
		return false;
	}

	private void movePlayer(){
		if(left){
			player.left();
		}

		if(right){
			player.right();
		}
	}



	public int getGameWidth() {
		return gameWidth;
	}

	public int getGameHeight() {
		return gameHeight;
	}

	public Player getPlayer() {
		return player;
	}
	public ConcreteScoreObs getObservers(){
		return this.scoreObserver;
	}
	@Override
	public void addObserver(Observer obs) {
		this.scoreObserver = (ConcreteScoreObs) obs;
	}

	@Override
	public void removeObserver(Observer obs) {
		this.scoreObserver = null;
	}

	@Override
	public void notifyObserver() {
		if(this.scoreObserver != null) {
			this.scoreObserver.update();
		}
	}





}
