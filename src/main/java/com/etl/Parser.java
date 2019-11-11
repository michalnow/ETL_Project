package com.etl;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Parser {

    public static void main(String[] args) throws InterruptedException {

        Scanner scanner = new Scanner(System.in);
        Document document = null;
            try {
            document = Jsoup.connect("https://www.ceneo.pl/76367847;02514#tab=reviews").get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Za 5 sekund nastąpi proces ETL, bądź gotów");
            Thread.sleep(4000);
        assert document != null;

        System.out.println("Extract !!!");
        Thread.sleep(1000);
        List<String> urls = prepareUrls(document);

        System.out.println("\n\nTransform !!!");
        Thread.sleep(1000);
        List<Opinion> opinions = generateOpinions(urls,document);

        System.out.println("\n\nLoad !!!");
        Thread.sleep(1000);
        loadOpinionsToDb(opinions);

    }

    private static void loadOpinionsToDb(List<Opinion> opinions) {
        String postUrl = "http://localhost:8080/api/opinion";
        for (Opinion opinion : opinions) {
            StringEntity postingString;
            try {
                Gson gson = new Gson();
                postingString = new StringEntity(gson.toJson(opinion));
                postingString.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
                        "application/json"));
                System.out.println(gson.toJson(opinion));
                HttpClient httpClient = HttpClientBuilder.create().build();

                HttpPost httpPost = new HttpPost(postUrl);
                httpPost.setHeader("Content-type","application/json");
                httpPost.setEntity(postingString);

                HttpResponse response = httpClient.execute(httpPost);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static List<String> prepareUrls(Document document){
        List<String> urls = new ArrayList<>();
        urls.add("https://www.ceneo.pl/76367847;02514#tab=reviews");

        while (true) {
            assert document != null;
            String next = document.select("li.arrow-next").select("a").attr("href");
            if (next.equals(""))
                break;
            String url = "https://www.ceneo.pl" + next;
            System.out.println("url = " + url);
            urls.add(url);
            try {
                document = Jsoup.connect(url).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return urls;
    }

    private static List<Opinion> generateOpinions(List<String> urls, Document document) {
        List<Opinion> opinions = new ArrayList<>();

        for (String url : urls) {
            try {
                document = Jsoup.connect(url).get();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Elements reviewsBody = document.select("li.review-box");
            for (Element review : reviewsBody) {
                Opinion opinion = new Opinion();
                opinion.setNickname(review.select("div.reviewer-name-line").text());
                opinion.setRecommendation(review.select("em.product-recommended").text()
                        .equals("Polecam") ? "Tak" : "Nie");

                String grade = review.select("span.review-score-count").text();
                opinion.setGrade(!grade.equals("") ? Integer.parseInt(grade.substring(0, 1)) : 0);
                opinion.setReview(review.select("p.product-review-body").text());

                String thumbsUp = review.select("button.vote-yes").attr("data-total-vote");
                String thumbsDown = review.select("button.vote-no").attr("data-total-vote");
                opinion.setThumbsUp(Integer.parseInt(!thumbsUp.equals("") ? thumbsUp : "0"));
                opinion.setThumbsDown(Integer.parseInt(!thumbsDown.equals("") ? thumbsDown : "0"));

                String date;
                date = review.select("time").attr("datetime").substring(0,10);
                opinion.setPublishDate(date);
                System.out.println(opinion);
                opinions.add(opinion);
            }
        }
        return opinions;
    }


}
