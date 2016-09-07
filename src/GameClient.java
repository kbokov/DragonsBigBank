/**
 * Created by kirillbokov on 06.09.16.
 */
import com.google.common.util.concurrent.ListenableFuture;

public interface GameClient {

    void close();

    /*void closeAsynchronously();/*/

    ListenableFuture<GameResponse> getGame();

    ListenableFuture<WeatherResponse> getWeather(Integer id);

    ListenableFuture<SolutionResponse> sendSolution(Integer id, SolutionRequest solutionRequest);

    SolutionRequest generateGameSolution(GameResponseItem gameResponseItem, WeatherResponse weatherResponse);

    GameCounters getAndSolveGames(int amountOfGames);
}
