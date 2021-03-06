/**
 * Created by kirillbokov on 06.09.16.
 */

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.ning.http.client.*;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncGameClient implements GameClient {
    private static final String BASE_URL = "http://www.dragonsofmugloar.com";
    private static final String WEATHER_URL = "/weather/api/report/";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JodaModule())
            .registerModule(new SimpleModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private static final int[][] SOLUTIONS = {
            {8, 8, 4, 0}, {10, 6, 3, 1},
            {8, 7, 5, 0}, {10, 5, 4, 1},
            {8, 6, 6, 0}, {10, 4, 4, 2},
            {7, 7, 6, 0}, {10, 4, 4, 2},
            {8, 8, 3, 1}, {10, 6, 2, 2},
            {8, 7, 4, 1}, {10, 5, 4, 1},
            {8, 6, 5, 1}, {10, 4, 5, 1},
            {7, 7, 5, 1}, {9, 5, 4, 2},
            {7, 6, 6, 1}, {9, 4, 4, 3},
            {8, 8, 2, 2}, {10, 6, 3, 1},
            {8, 7, 3, 2}, {10, 5, 4, 1},
            {8, 6, 4, 2}, {10, 4, 3, 3},
            {7, 7, 4, 2}, {10, 5, 4, 1},
            {8, 5, 5, 2}, {10, 5, 4, 1},
            {7, 6, 5, 2}, {10, 4, 4, 2},
            {6, 6, 6, 2}, {10, 4, 3, 3},
            {8, 6, 3, 3}, {10, 4, 4, 2},
            {7, 7, 3, 3}, {10, 4, 4, 2},
            {8, 5, 4, 3}, {10, 4, 4, 2},
            {7, 6, 4, 3}, {9, 4, 5, 2},
            {7, 5, 5, 3}, {9, 5, 4, 2},
            {6, 6, 5, 3}, {8, 5, 4, 3},
            {8, 4, 4, 4}, {10, 4, 3, 3},
            {7, 5, 4, 4}, {10, 4, 3, 3},
            {6, 6, 4, 4}, {10, 4, 3, 3},
            {6, 5, 5, 4}, {10, 4, 3, 3},
            {5, 5, 5, 5}, {10, 4, 3, 3}
    };

    private final AsyncHttpClient httpClient;
    private final String baseUrl;

    private AsyncGameClient(
            final AsyncHttpClient httpClient,
            final String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }

    private static AsyncHttpClientConfig commonSetup(final AsyncHttpClientConfig.Builder configBuilder) {
        final Realm realm = new Realm.RealmBuilder().build();
        configBuilder.setRealm(realm);
        return configBuilder.build();
    }

    public static AsyncGameClient createDefault() {
        return new AsyncGameClient(
                new AsyncHttpClient(commonSetup(new Builder())), BASE_URL);
    }

    public static AsyncGameClient create(final AsyncHttpClientConfig config) {
        return new AsyncGameClient(
                new AsyncHttpClient(commonSetup(new Builder(config))), BASE_URL);
    }

    @Override
    public void close() {
        this.httpClient.close();
    }

    private BoundRequestBuilder get(final String resourceUrl) {

        return this.httpClient.prepareGet(this.baseUrl + resourceUrl);
    }

    private BoundRequestBuilder put(final String resourceUrl, final HasParams hasParams) {
        final BoundRequestBuilder builder = this.httpClient.preparePut(this.baseUrl + resourceUrl);
        final Map<String, Object> params = hasParams.getParams();
        try {
            final String objectAsString = MAPPER.writeValueAsString(params);
            builder.addHeader("Content-Type", "application/json; charset=utf-8");
            builder.setBody(objectAsString);
        } catch (Exception ignore) {
        }
        return builder;
    }

    @Override
    public ListenableFuture<GameResponse> getGame() {
        return execute(GameResponse.class, get("/api/game"));
    }

    @Override
    public ListenableFuture<WeatherResponse> getWeather(Integer id) {
        return execute(WeatherResponse.class, get(WEATHER_URL + id));
    }

    @Override
    public ListenableFuture<SolutionResponse> sendSolution(Integer id, SolutionRequest solutionRequest) {
        return execute(SolutionResponse.class, put("/api/game/" + id + "/solution", solutionRequest));
    }

    @Override
    public SolutionRequest generateGameSolution(GameResponseItem gameResponseItem, WeatherResponse weatherResponse) {
        if ("T E".equals(weatherResponse.getCode())) {
            return SolutionRequest.builder()
                    .scale(5)
                    .claw(5)
                    .wing(5)
                    .fire(5)
                    .build();
        } else if ("HVA".equals(weatherResponse.getCode())) {
            return SolutionRequest.builder()
                    .scale(10)
                    .claw(10)
                    .wing(0)
                    .fire(0)
                    .build();
        }

        final List<Integer> knightAttrs = Arrays.asList(gameResponseItem.getAttack(),
                gameResponseItem.getArmor(), gameResponseItem.getAgility(), gameResponseItem.getEndurance());
        final Integer[] indexes = {0, 1, 2, 3};

        Arrays.sort(indexes, new Comparator<Integer>() {
            @Override
            public int compare(final Integer o1, final Integer o2) {
                return knightAttrs.get(o1).compareTo(knightAttrs.get(o2));
            }
        });
        int maxIndex = indexes[3];
        int secondMaxIndex = indexes[2];
        int thirdMaxIndex = indexes[1];
        int forthMaxIndex = indexes[0];
        int[] dragonAttrs = new int[]{0, 0, 0, 0};
        for (int index = 0; index < SOLUTIONS.length; index += 2) {
            if (knightAttrs.get(maxIndex) == SOLUTIONS[index][0]
                    && knightAttrs.get(secondMaxIndex) == SOLUTIONS[index][1]
                    && knightAttrs.get(thirdMaxIndex) == SOLUTIONS[index][2]
                    && knightAttrs.get(forthMaxIndex) == SOLUTIONS[index][3]) {
                dragonAttrs[maxIndex] = SOLUTIONS[index + 1][0];
                dragonAttrs[secondMaxIndex] = SOLUTIONS[index + 1][1];
                dragonAttrs[thirdMaxIndex] = SOLUTIONS[index + 1][2];
                dragonAttrs[forthMaxIndex] = SOLUTIONS[index + 1][3];
                break;
            }
        }
        final SolutionRequest request = SolutionRequest.builder()
                .scale(dragonAttrs[0])
                .claw(dragonAttrs[1])
                .wing(dragonAttrs[2])
                .fire(dragonAttrs[3])
                .build();
        return request;
    }

    private static class CallableImpl implements Callable<Void> {

        private final AsyncGameClient asyncClient;
        private final GameCounters gameCounters;

        public CallableImpl(AsyncGameClient asyncClient, GameCounters gameCounters) {
            this.asyncClient = asyncClient;
            this.gameCounters = gameCounters;
        }

        public Void call() {
            try {
                final GameResponse game = asyncClient.getGame().get();
                final WeatherResponse weatherResponse = asyncClient.getWeather(game.getGameId()).get();
                if ("SRO".equals(weatherResponse.getCode())) {
                    gameCounters.getStormCount().getAndIncrement();
                } else {
                    final SolutionRequest request = asyncClient.generateGameSolution(game.getGameResponseItem(), weatherResponse);
                    final SolutionResponse response = asyncClient.sendSolution(game.getGameId(), request).get();
                    if ("Victory".equals(response.getStatus())) {
                        gameCounters.getVictoryCount().getAndIncrement();
                    }
                }
            } catch (Exception ex) {
                gameCounters.getErrorCount().getAndIncrement();
            }
            return null;
        }
    }

    @Override
    public GameCounters getAndSolveGames(int amountOfGames) {
        final GameCounters gameCounters = new GameCounters();
        final ExecutorService executor = Executors.newFixedThreadPool(100);
        final List<Callable<Void>> callables = new ArrayList<Callable<Void>>();
        for (int gameIndex = 0; gameIndex < amountOfGames; gameIndex += 1) {
            callables.add(new CallableImpl(this, gameCounters));
        }
        try {
            executor.invokeAll(callables);
        } catch (InterruptedException ex) {
        }
        executor.shutdown();
        return gameCounters;
    }

    private static <T> ListenableFuture<T> execute(
            final Class<T> clazz,
            final BoundRequestBuilder request) {
        final SettableFuture<T> guavaFut = SettableFuture.create();
        try {
            request.execute(new GuavaFutureConverter<T>(clazz, guavaFut));
        } catch (final IOException e) {
            guavaFut.setException(e);
        }
        return guavaFut;
    }

    private static class GuavaFutureConverter<T> extends AsyncCompletionHandler<T> {
        private final Class<T> clazz;
        private final SettableFuture<T> guavaFut;

        public GuavaFutureConverter(
                final Class<T> clazz,
                final SettableFuture<T> guavaFut) {
            this.clazz = clazz;
            this.guavaFut = guavaFut;
        }

        private static boolean isSuccess(final Response response) {
            final int statusCode = response.getStatusCode();
            return statusCode > 199 && statusCode < 400;
        }

        @Override
        public void onThrowable(final Throwable t) {
            guavaFut.setException(t);
        }

        @Override
        public T onCompleted(final Response response) throws Exception {
            if (isSuccess(response)) {
                final T value = clazz == WeatherResponse.class ? XML_MAPPER.readValue(response.getResponseBody(), clazz)
                        : MAPPER.readValue(response.getResponseBody(), clazz);
                guavaFut.set(value);
                return value;
            }
            throw new UnsupportedOperationException(response.getStatusCode() + " - " + response.getResponseBody());
        }
    }

}

