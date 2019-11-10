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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class Parser {

    public static void main(String[] args) {

        ArrayList<String> urls = new ArrayList<String>();
        ArrayList<Opinion> opinions = new ArrayList<Opinion>();
        urls.add("https://www.ceneo.pl/76367847;02514#tab=reviews");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Document document = null;
        try {
            document = Jsoup.connect("https://www.ceneo.pl/76367847;02514#tab=reviews").get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (!document.select("li.arrow-next").equals("")) {
            String next = document.select("li.arrow-next").select("a").attr("href");
            if (next.equals(""))
                break;
            String url = "https://www.ceneo.pl" + next;
            System.out.println(url);
            urls.add(url);
            try {
                document = Jsoup.connect(url).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Elements hrefs = document.select("li.arrow-next");
        for (Element href : hrefs) {
            urls.add("https://www.ceneo.pl" + href.select("a").attr("href"));
            System.out.println(href);
        }

        for (String url : urls) {
            try {
                document = Jsoup.connect(url).get();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Elements reviewesBody = document.select("li.review-box");
            for (Element review : reviewesBody) {
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
                    //System.out.println(format.parse((review.select("time").attr("datetime"))));
                opinion.setPublishDate(date);

              /*  Elements advantages = document.select("div.pros-cell");
                for(Element advantage: advantages){

                    if(advantage.select("li").size() !=0){
                        opinion.setAdvantage(advantage.select("li").text());
                    }
                }*/

                opinions.add(opinion);
            }

        }

        String postUrl = "http://localhost:8080/api/opinion";
        for (Opinion opinion : opinions) {
            StringEntity postingString;
            try {
                Gson gson = new Gson();
                postingString = new StringEntity(gson.toJson(opinion));
                postingString.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
                        "application/json"));
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
}
