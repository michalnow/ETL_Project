package com.etl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Parser {

    public static void main(String[] args){

        ArrayList<String> urls = new ArrayList<String>();
        ArrayList<Opinion> opinions = new ArrayList<Opinion>();
        urls.add("https://www.ceneo.pl/76367847;02514#tab=reviews");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Document document = null;
        try {
             document = Jsoup.connect("https://www.ceneo.pl/76367847;02514#tab=reviews").get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(!document.select("li.arrow-next").equals("")){
            String next = document.select("li.arrow-next").select("a").attr("href");
            if(next.equals(""))
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
        for(Element href: hrefs){
            urls.add("https://www.ceneo.pl" + href.select("a").attr("href"));
            System.out.println(href);
        }

        for(String url: urls) {
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
                        .equals("Polecam"));
                String grade = review.select("span.review-score-count").text();
                opinion.setGrade(!grade.equals("") ? Integer.parseInt(grade.substring(0, 1)) : 0);
                opinion.setReview(review.select("p.product-review-body").text());

                String thumbsUp = review.select("button.vote-yes").attr("data-total-vote");
                String thumbsDown = review.select("button.vote-no").attr("data-total-vote");
                opinion.setThumbsUp(Integer.parseInt(!thumbsUp.equals("") ? thumbsUp : "0"));
                opinion.setThumbsDown(Integer.parseInt(!thumbsDown.equals("") ? thumbsDown : "0"));

                Date date = null;
                try {
                    date = format.parse((review.select("time").attr("datetime")));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
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
        for(Opinion opinion: opinions){ System.out.println("nickname : " + opinion.getNickname());
            System.out.println("review : " + opinion.getReview());
            System.out.println("date : " + opinion.getPublishDate());
            System.out.println("grade: " + opinion.getGrade());
            System.out.println("recommended : " + opinion.isRecommendation());
            System.out.println("Thumbs up : " + opinion.getThumbsUp());
            System.out.println("Thumbs down : " + opinion.getThumbsDown());
          //  System.out.println("pro: " + opinion.getAdvantage());
        }

    }
}
