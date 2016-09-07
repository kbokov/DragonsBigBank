/**
 * Created by kirillbokov on 06.09.16.
 */

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.ning.http.client.AsyncHttpClient;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class Main {

    private static AsyncGameClient gameClient;


    public static void main(String... args) {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());

        //creating an object of the class AsyncGameClient
        gameClient = AsyncGameClient.createDefault();



        GamePackage gamePackage = new GamePackage();

        ListenableFuture<GameResponse> futureGame = gameClient.getGame(); // getting the game

        //asking for weather after receiving the game
        ListenableFuture<WeatherResponse> futureWeatherResponse = Futures.transformAsync(futureGame, getWeatherAsyncFunc(gamePackage));
        //after receiving the weather, sending request for solution
        ListenableFuture<SolutionResponse> futureSolutionResponse = Futures.transformAsync(futureWeatherResponse, sendSolutionFunc(gamePackage));

        //as long as solution was sent - calling getSolutionReceivedCallback.
        Futures.addCallback(futureSolutionResponse, getSolutionReceivedCallback(gamePackage));


    }

    public static AsyncFunction<GameResponse, WeatherResponse> getWeatherAsyncFunc(final GamePackage gamePackage){
        return new AsyncFunction<GameResponse, WeatherResponse>() {
            @Override
            public ListenableFuture<WeatherResponse> apply(@Nullable GameResponse gameResponse) throws Exception {
                gamePackage.setGameResponse(gameResponse); //adding received information from API
                return gameClient.getWeather(gameResponse.getGameId());
            }
        };
    }

    static AsyncFunction<WeatherResponse, SolutionResponse> sendSolutionFunc(final GamePackage gamePackage){
        return new AsyncFunction<WeatherResponse, SolutionResponse>() {
            @Override
            public ListenableFuture<SolutionResponse> apply(@Nullable WeatherResponse weatherResponse) throws Exception {
                gamePackage.setWeatherResponse(weatherResponse); //adding missing info

                SolutionRequest solutionRequest = gameClient.generateGameSolution(gamePackage.getGameResponse().getGameResponseItem(), weatherResponse);
                return gameClient.sendSolution(gamePackage.getGameResponse().getGameId(), solutionRequest);
            }
        };

    }

    static FutureCallback<SolutionResponse> getSolutionReceivedCallback(final GamePackage gamePackage){
        return new FutureCallback<SolutionResponse>() {
            @Override
            public void onSuccess(@Nullable SolutionResponse solutionResponse) {
                if ("Victory".equals(solutionResponse.getStatus())) {
                    // We win a game
                    System.out.println("Victory");
                } else if ("SRO".equals(gamePackage.getWeatherResponse().getCode())) {
                    // Storm weather
                    System.out.println("shit");
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                System.out.println("fail");
            }
        };}
}
