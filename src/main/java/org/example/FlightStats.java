package org.example;

import com.google.gson.*;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FlightStats {
    public static void main(String[] args) {
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                    .registerTypeAdapter(LocalTime.class, new LocalTimeTypeAdapter())
                    .create();
            TicketList ticketList = gson.fromJson(new FileReader("tickets.json"), TicketList.class);

            Map<String, Long> minFlightTimes = getMinFlightTime(ticketList);

            System.out.println("Минимальное время полета между городами Владивосток и Тель-Авив:");
            for (Map.Entry<String, Long> entry : minFlightTimes.entrySet()) {
                System.out.println("Авиаперевозчик: " + entry.getKey() + ", Время полета: "
                        + LocalTime.ofSecondOfDay(entry.getValue()));
            }

            List<Double> flightPrices = ticketList.tickets.stream()
                    .map(t -> (double) t.price)
                    .collect(Collectors.toList());

            double medianPrice = calculateMedian(flightPrices);
            double averagePrice = flightPrices.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double difference = averagePrice - medianPrice;

            System.out.println();
            System.out.println("Разница между средней ценой и медианой для полета между Владивостоком и Тель-Авивом:");
            System.out.println("Средняя цена: " + averagePrice);
            System.out.println("Медианная цена: " + medianPrice);
            System.out.println("Разница: " + difference);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double calculateMedian(List<Double> prices) {
        List<Double> sortedPrices = prices.stream().sorted().toList();
        int size = sortedPrices.size();
        if (size % 2 == 0) {
            return (sortedPrices.get(size / 2 - 1) + sortedPrices.get(size / 2)) / 2;
        } else {
            return sortedPrices.get(size / 2);
        }
    }

    private static Map<String, Long> getMinFlightTime(TicketList tickets) {
        Map<String, Long> minFlightTimes = new HashMap<>();
        for (FlightTicket ticket : tickets.tickets) {
            String carrier = ticket.carrier;
            long flightTime = Duration.between((LocalDateTime.of(ticket.departure_date, ticket.departure_time)),
                    LocalDateTime.of(ticket.arrival_date, ticket.arrival_time)).getSeconds();

            if (!minFlightTimes.containsKey(carrier) || flightTime < minFlightTimes.get(carrier)) {
                minFlightTimes.put(carrier, flightTime);
            }
        }
        return minFlightTimes;
    }

    private record FlightTicket(String carrier, String origin, String destination, LocalDate departure_date,
                                LocalTime departure_time, LocalDate arrival_date, LocalTime arrival_time, int price) {
    }

    private record TicketList(List<FlightTicket> tickets) {
    }

    public static class LocalDateTypeAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");

        @Override
        public JsonElement serialize(final LocalDate date, final Type typeOfSrc,
                                     final JsonSerializationContext context) {
            return new JsonPrimitive(date.format(formatter));
        }

        @Override
        public LocalDate deserialize(final JsonElement json, final Type typeOfT,
                                     final JsonDeserializationContext context) throws JsonParseException {
            return LocalDate.parse(json.getAsString(), formatter);
        }
    }

    public static class LocalTimeTypeAdapter implements JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
        private final DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("HH:mm");
        private final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("H:mm");


        @Override
        public JsonElement serialize(final LocalTime time, final Type typeOfSrc,
                                     final JsonSerializationContext context) {
            return new JsonPrimitive(time.format(formatter1));
        }

        @Override
        public LocalTime deserialize(final JsonElement json, final Type typeOfT,
                                     final JsonDeserializationContext context) throws JsonParseException {
            try {
                return LocalTime.parse(json.getAsString(), formatter1);
            } catch (DateTimeParseException e) {
                return LocalTime.parse(json.getAsString(), formatter2);
            }
        }
    }
}
