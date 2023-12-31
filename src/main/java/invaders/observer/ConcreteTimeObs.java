package invaders.observer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class ConcreteTimeObs implements Observer{
    int second = 0;
    int minute = 0;
    int millis = 0;

    public void setMillis(int millis){this.millis = millis;}
    public void setMinute(int time){
        this.minute = time;
    }
    public void setSecond(int time){
        this.second = time;
    }
    public int getSecond(){
        return this.second;
    }
    public int getMillis(){
        return this.millis;
    }
    public int getMinute(){
        return this.minute;
    }
    public String toString(){
        return String.format("%02d:%02d", minute, second);
    }
    @Override
    public void update(){
        millis ++;
        if(millis == 120){
            second ++;
            millis = 0;
        }
        if(second == 60){
            minute ++;
            second = 0;
        }
    }
}
