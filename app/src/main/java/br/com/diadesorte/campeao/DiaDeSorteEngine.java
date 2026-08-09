package br.com.diadesorte.campeao;

import java.util.*;

public final class DiaDeSorteEngine {
    private DiaDeSorteEngine(){}

    public static final String[] MESES = {
        "", "JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO",
        "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"
    };

    public static final class Contest {
        public final int[] nums;
        public final int month;
        Contest(int[] nums, int month){
            this.nums = nums;
            this.month = month;
        }
    }

    public static final class Model {
        public final ArrayList<Contest> contests;
        public final Contest last;
        Model(ArrayList<Contest> contests){
            this.contests = contests;
            this.last = contests.get(contests.size() - 1);
        }
    }

    public static final class Result {
        public final String name;
        public final String detail;
        public final int[] game;
        public final int month;
        Result(String name, String detail, int[] game, int month){
            this.name = name;
            this.detail = detail;
            this.game = game;
            this.month = month;
        }
    }

    public static Contest parse(String line){
        if(line == null) return null;

        java.util.regex.Matcher matcher =
            java.util.regex.Pattern.compile("\\b(0?[1-9]|[12][0-9]|3[01])\\b").matcher(line);

        ArrayList<Integer> values = new ArrayList<>();
        while(matcher.find()){
            values.add(Integer.parseInt(matcher.group()));
        }

        if(values.size() < 7) return null;

        int[] nums = new int[7];
        int start = values.size() - 7;

        for(int i = 0; i < 7; i++){
            nums[i] = values.get(start + i);
        }

        Arrays.sort(nums);

        for(int i = 1; i < nums.length; i++){
            if(nums[i] == nums[i - 1]) return null;
        }

        return new Contest(nums, parseMonth(line));
    }

    private static int parseMonth(String line){
        String u = line.toUpperCase(Locale.ROOT);

        String[][] aliases = {
            {"JANEIRO"},
            {"FEVEREIRO"},
            {"MARÇO","MARCO"},
            {"ABRIL"},
            {"MAIO"},
            {"JUNHO"},
            {"JULHO"},
            {"AGOSTO"},
            {"SETEMBRO"},
            {"OUTUBRO"},
            {"NOVEMBRO"},
            {"DEZEMBRO"}
        };

        for(int i = 0; i < aliases.length; i++){
            for(String alias : aliases[i]){
                if(u.contains(alias)){
                    return i + 1;
                }
            }
        }

        java.util.regex.Matcher m =
            java.util.regex.Pattern.compile(
                "(?:M[EÊ]S|MES)\\s*[:=-]?\\s*(1[0-2]|[1-9])",
                java.util.regex.Pattern.CASE_INSENSITIVE
            ).matcher(line);

        if(m.find()){
            return Integer.parseInt(m.group(1));
        }

        return 0;
    }

    private static boolean contains(int[] a, int n){
        return Arrays.binarySearch(a, n) >= 0;
    }

    private static double[] baseScore(Model m, int recentWindow){
        double[] score = new double[32];
        int recentStart = Math.max(0, m.contests.size() - recentWindow);

        for(int i = 0; i < m.contests.size(); i++){
            for(int n : m.contests.get(i).nums){
                score[n] += 1.0;
                if(i >= recentStart) score[n] += 2.5;
            }
        }

        for(int n = 1; n <= 31; n++){
            int gap = 0;
            for(int i = m.contests.size() - 1; i >= 0; i--){
                if(contains(m.contests.get(i).nums, n)) break;
                gap++;
            }
            score[n] += Math.min(gap, 12) * 0.18;
        }

        return score;
    }

    private static int[] top(double[] score, int k){
        ArrayList<Integer> nums = new ArrayList<>();
        for(int n = 1; n <= 31; n++) nums.add(n);

        nums.sort((a, b) -> Double.compare(score[b], score[a]));

        int[] game = new int[k];
        for(int i = 0; i < k; i++) game[i] = nums.get(i);

        Arrays.sort(game);
        return game;
    }

    public static Result module1(Model m){
        return new Result(
            "MÓDULO 1 — NÚMEROS CAMPEÕES",
            "Analisa frequência histórica, últimos 60 concursos, atraso e retorno das 31 dezenas.",
            top(baseScore(m, 60), 7),
            bestMonth(m)
        );
    }

    public static Result module2(Model m){
        double[] score = baseScore(m, 80);
        int[][] pairs = new int[32][32];

        for(Contest c : m.contests){
            for(int i = 0; i < 7; i++){
                for(int j = i + 1; j < 7; j++){
                    pairs[c.nums[i]][c.nums[j]]++;
                }
            }
        }

        for(int n = 1; n <= 31; n++){
            double pairPower = 0;
            for(int other = 1; other <= 31; other++){
                if(other == n) continue;
                int a = Math.min(n, other);
                int b = Math.max(n, other);
                pairPower += pairs[a][b];
            }
            score[n] += pairPower * 0.10;
        }

        HashMap<String, Integer> trios = new HashMap<>();

        for(Contest c : m.contests){
            int[] d = c.nums;

            for(int i = 0; i < 7; i++){
                for(int j = i + 1; j < 7; j++){
                    for(int k = j + 1; k < 7; k++){
                        String key = d[i] + "-" + d[j] + "-" + d[k];
                        trios.put(key, trios.getOrDefault(key, 0) + 1);
                    }
                }
            }
        }

        for(Map.Entry<String, Integer> e : trios.entrySet()){
            int weight = e.getValue();

            for(String part : e.getKey().split("-")){
                score[Integer.parseInt(part)] += weight * 0.55;
            }
        }

        return new Result(
            "MÓDULO 2 — DUPLAS E TRIOS",
            "Avalia as 465 duplas possíveis e os 4.495 trios possíveis, cruzando recorrência e fase recente.",
            top(score, 7),
            bestMonth(m)
        );
    }

    public static Result module3(Model m){
        int[] windows = {5, 10, 20, 40};
        double[] score = new double[32];
        StringBuilder detail = new StringBuilder("PERÍMETRO E EVOLUÇÃO\n");

        for(int w : windows){
            int[] count = new int[32];

            for(int i = Math.max(0, m.contests.size() - w); i < m.contests.size(); i++){
                for(int n : m.contests.get(i).nums){
                    count[n]++;
                }
            }

            for(int n = 1; n <= 31; n++){
                score[n] += count[n] * (50.0 / w);
            }

            ArrayList<Integer> rank = new ArrayList<>();
            for(int n = 1; n <= 31; n++) rank.add(n);

            rank.sort((a, b) -> Integer.compare(count[b], count[a]));

            detail.append("Últimos ").append(w).append(": ");

            for(int i = 0; i < 7; i++){
                detail.append(String.format(Locale.US, "%02d", rank.get(i)));
                if(i < 6) detail.append(" ");
            }

            detail.append("\n");
        }

        return new Result(
            "MÓDULO 3 — PERÍMETRO E EVOLUÇÃO",
            detail.toString(),
            top(score, 7),
            bestMonth(m)
        );
    }

    public static Result module4(Model m){
        int[] hist = new int[13];
        int[] rec20 = new int[13];
        int[] rec50 = new int[13];

        for(int i = 0; i < m.contests.size(); i++){
            int month = m.contests.get(i).month;

            if(month < 1 || month > 12) continue;

            hist[month]++;

            if(i >= Math.max(0, m.contests.size() - 20)) rec20[month]++;
            if(i >= Math.max(0, m.contests.size() - 50)) rec50[month]++;
        }

        Integer[] ranking = new Integer[12];
        for(int i = 0; i < 12; i++) ranking[i] = i + 1;

        Arrays.sort(ranking, (a, b) -> Double.compare(
            monthScore(b, hist, rec20, rec50),
            monthScore(a, hist, rec20, rec50)
        ));

        int chosen = ranking[0];

        StringBuilder detail = new StringBuilder("ESTUDO DO MÊS DA SORTE\n");
        detail.append("Maior peso: últimos 20; depois últimos 50; histórico como reforço.\n");
        detail.append("Recomendado: ").append(monthName(chosen)).append("\n");
        detail.append("TOP 5 MESES\n");

        for(int i = 0; i < 5; i++){
            int mth = ranking[i];

            detail.append(i + 1).append("º ")
                  .append(monthName(mth))
                  .append(" • ult20=").append(rec20[mth])
                  .append(" • ult50=").append(rec50[mth])
                  .append(" • hist=").append(hist[mth])
                  .append("\n");
        }

        return new Result(
            "MÓDULO 4 — MÊS DA SORTE",
            detail.toString(),
            module1(m).game,
            chosen
        );
    }

    public static Result module5(Model m){
        Result[] results = {
            module1(m),
            module2(m),
            module3(m)
        };

        double[] vote = new double[32];
        StringBuilder detail = new StringBuilder("CONSENSO DOS MÉTODOS\n");

        for(Result r : results){
            double quality = recentBacktest(m, r.game);
            double weight = 1.0 + quality;

            for(int n : r.game){
                vote[n] += weight;
            }

            detail.append(r.name)
                  .append(" • nota ")
                  .append(String.format(Locale.US, "%.2f", quality))
                  .append(" • ")
                  .append(join(r.game))
                  .append("\n");
        }

        int month = bestMonth(m);

        detail.append("\nMês da Sorte recomendado: ")
              .append(monthName(month));

        return new Result(
            "MÓDULO 5 — CAMPEÃO GERAL",
            detail.toString(),
            top(vote, 7),
            month
        );
    }

    private static double recentBacktest(Model m, int[] game){
        int start = Math.max(0, m.contests.size() - 100);
        double score = 0;
        int count = 0;

        for(int i = start; i < m.contests.size(); i++){
            int hits = 0;

            for(int n : game){
                if(contains(m.contests.get(i).nums, n)) hits++;
            }

            if(hits == 7) score += 25;
            else if(hits == 6) score += 12;
            else if(hits == 5) score += 6;
            else if(hits == 4) score += 3;
            else if(hits == 3) score += 1.5;
            else score += hits * 0.15;

            count++;
        }

        return score / Math.max(1, count);
    }

    private static double monthScore(int month, int[] hist, int[] rec20, int[] rec50){
        return rec20[month] * 5.0 + rec50[month] * 2.0 + hist[month] * 0.35;
    }

    public static int bestMonth(Model m){
        int[] hist = new int[13];
        int[] rec20 = new int[13];
        int[] rec50 = new int[13];

        for(int i = 0; i < m.contests.size(); i++){
            int month = m.contests.get(i).month;

            if(month < 1 || month > 12) continue;

            hist[month]++;

            if(i >= Math.max(0, m.contests.size() - 20)) rec20[month]++;
            if(i >= Math.max(0, m.contests.size() - 50)) rec50[month]++;
        }

        int best = 1;
        double bestScore = -1;

        for(int month = 1; month <= 12; month++){
            double score = monthScore(month, hist, rec20, rec50);

            if(score > bestScore){
                bestScore = score;
                best = month;
            }
        }

        return best;
    }

    public static String monthName(int month){
        if(month < 1 || month > 12) return "NÃO IDENTIFICADO";
        return MESES[month];
    }

    public static String join(int[] game){
        StringBuilder out = new StringBuilder();

        for(int i = 0; i < game.length; i++){
            if(i > 0) out.append(" ");
            out.append(String.format(Locale.US, "%02d", game[i]));
        }

        return out.toString();
    }
}
