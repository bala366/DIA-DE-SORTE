package br.com.diadesorte.campeao;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    static final int BROWN=Color.rgb(122,75,42);
    static final int DARK=Color.rgb(78,46,25);
    static final int TEXT=Color.rgb(32,33,36);

    final ExecutorService executor=Executors.newSingleThreadExecutor();

    LinearLayout root, content, results;
    TextView status, lastInfo;

    DiaDeSorteEngine.Model model;
    DiaDeSorteEngine.Result current;

    @Override
    public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(DARK);
        buildScreen();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        executor.shutdownNow();
    }

    void buildScreen(){
        ScrollView scroll=new ScrollView(this);

        root=column();
        root.setPadding(dp(16),dp(12),dp(16),dp(40));
        scroll.addView(root);

        LinearLayout header=column();
        header.setPadding(dp(18),dp(16),dp(18),dp(16));
        header.setBackgroundColor(BROWN);
        header.addView(label("☘ DIA DE SORTE CAMPEÃO",28,true,Color.WHITE));
        header.addView(label("7 números • Mês da Sorte • jogo + PDF",15,false,Color.WHITE));
        root.addView(header);

        title("BASE DE RESULTADOS");

        Button load=button("SELECIONAR TXT DO DIA DE SORTE");
        root.addView(load,new LinearLayout.LayoutParams(-1,dp(62)));

        status=label("Carregue o TXT com os concursos.",15,false,TEXT);
        root.addView(status);

        lastInfo=label("Último concurso: —",14,true,TEXT);
        root.addView(lastInfo);

        title("MÓDULOS");

        String[] names={
            "NÚMEROS CAMPEÕES",
            "DUPLAS E TRIOS",
            "PERÍMETRO E EVOLUÇÃO",
            "MÊS DA SORTE",
            "CAMPEÃO GERAL"
        };

        for(int i=0;i<5;i++){
            Button b=button("MÓDULO "+(i+1)+" — "+names[i]);
            final int module=i+1;

            root.addView(b,new LinearLayout.LayoutParams(-1,dp(64)));
            b.setOnClickListener(v->showModule(module));
        }

        content=column();
        root.addView(content);

        load.setOnClickListener(v->pickTxt());

        showModule(1);

        setContentView(scroll);
    }

    void showModule(int module){
        content.removeAllViews();

        String[] descriptions={
            "",
            "Analisa as 31 dezenas por frequência, fase recente, atraso e retorno.",
            "Analisa as 465 duplas e os 4.495 trios possíveis.",
            "Mede as janelas de 5, 10, 20 e 40 concursos.",
            "Ranqueia os 12 meses, com peso maior para os concursos recentes.",
            "Cruza os módulos e gera o jogo campeão."
        };

        TextView h=label("MÓDULO "+module,21,true,TEXT);
        h.setPadding(0,dp(16),0,dp(5));
        content.addView(h);

        content.addView(label(descriptions[module],15,false,TEXT));

        Button run=button("EXECUTAR ESTUDO E GERAR JOGO");
        content.addView(run,new LinearLayout.LayoutParams(-1,dp(68)));

        results=column();
        content.addView(results);

        run.setOnClickListener(v->{
            if(model==null){
                toast("Carregue primeiro o TXT.");
                return;
            }

            run.setEnabled(false);
            results.removeAllViews();

            executor.submit(()->{
                try{
                    DiaDeSorteEngine.Result result;

                    if(module==1) result=DiaDeSorteEngine.module1(model);
                    else if(module==2) result=DiaDeSorteEngine.module2(model);
                    else if(module==3) result=DiaDeSorteEngine.module3(model);
                    else if(module==4) result=DiaDeSorteEngine.module4(model);
                    else result=DiaDeSorteEngine.module5(model);

                    current=result;

                    runOnUiThread(()->{
                        results.addView(label(result.detail,14,false,TEXT));

                        results.addView(
                            label(
                                "\nJOGO FINAL — 7 NÚMEROS\n"+
                                DiaDeSorteEngine.join(result.game),
                                22,true,DARK
                            )
                        );

                        results.addView(
                            label(
                                "\nMÊS DA SORTE\n"+
                                DiaDeSorteEngine.monthName(result.month),
                                20,true,BROWN
                            )
                        );

                        results.addView(volante(result.game));

                        Button pdf=button("SALVAR VOLANTE PDF COLORIDO");
                        results.addView(pdf,new LinearLayout.LayoutParams(-1,dp(64)));
                        pdf.setOnClickListener(x->savePdf());

                        status.setText("Estudo concluído. Jogo e mês gerados.");
                        run.setEnabled(true);
                    });

                }catch(Exception e){
                    runOnUiThread(()->{
                        status.setText("Erro: "+e.getMessage());
                        run.setEnabled(true);
                    });
                }
            });
        });
    }

    View volante(int[] game){
        GridLayout grid=new GridLayout(this);
        grid.setColumnCount(7);

        boolean[] selected=new boolean[32];
        for(int n:game) selected[n]=true;

        for(int n=1;n<=31;n++){
            TextView cell=label(
                String.format(Locale.US,"%02d",n),
                12,
                true,
                selected[n]?Color.WHITE:TEXT
            );

            cell.setGravity(Gravity.CENTER);
            cell.setBackgroundColor(
                selected[n]?BROWN:Color.rgb(240,240,240)
            );

            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();
            lp.width=0;
            lp.height=dp(40);
            lp.columnSpec=GridLayout.spec((n-1)%7,1f);
            lp.setMargins(dp(1),dp(1),dp(1),dp(1));

            grid.addView(cell,lp);
        }

        return grid;
    }

    void pickTxt(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent,1001);
    }

    @Override
    protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);

        if(request==1001 && result==RESULT_OK && data!=null){
            loadTxt(data.getData());
        }
    }

    void loadTxt(Uri uri){
        status.setText("Carregando...");

        executor.submit(()->{
            ArrayList<DiaDeSorteEngine.Contest> contests=new ArrayList<>();

            try(
                BufferedReader reader=new BufferedReader(
                    new InputStreamReader(
                        getContentResolver().openInputStream(uri),
                        StandardCharsets.UTF_8
                    )
                )
            ){
                String line;

                while((line=reader.readLine())!=null){
                    DiaDeSorteEngine.Contest c=DiaDeSorteEngine.parse(line);
                    if(c!=null) contests.add(c);
                }

                if(contests.size()<10){
                    throw new Exception("Poucos concursos válidos no TXT.");
                }

                model=new DiaDeSorteEngine.Model(contests);

                runOnUiThread(()->{
                    status.setText(
                        "Base carregada: "+
                        contests.size()+
                        " concursos."
                    );

                    lastInfo.setText(
                        "Últimos números: "+
                        DiaDeSorteEngine.join(model.last.nums)+
                        "\nÚltimo mês: "+
                        DiaDeSorteEngine.monthName(model.last.month)
                    );
                });

            }catch(Exception e){
                runOnUiThread(
                    ()->status.setText("Erro: "+e.getMessage())
                );
            }
        });
    }

    void savePdf(){
        if(current==null){
            toast("Gere primeiro o jogo.");
            return;
        }

        try{
            PdfDocument document=new PdfDocument();

            PdfDocument.Page page=document.startPage(
                new PdfDocument.PageInfo.Builder(595,842,1).create()
            );

            Canvas canvas=page.getCanvas();
            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);

            paint.setColor(BROWN);
            canvas.drawRect(0,0,595,92,paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(25);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText("DIA DE SORTE CAMPEÃO",30,42,paint);

            paint.setTextSize(12);
            canvas.drawText(current.name,30,68,paint);

            boolean[] selected=new boolean[32];
            for(int n:current.game) selected[n]=true;

            float startX=45;
            float startY=120;
            float cellW=70;
            float cellH=46;

            for(int row=0;row<5;row++){
                for(int col=0;col<7;col++){
                    int n=row*7+col+1;
                    if(n>31) continue;

                    float x=startX+col*cellW;
                    float y=startY+row*cellH;

                    paint.setColor(
                        selected[n]?BROWN:Color.rgb(238,238,238)
                    );

                    canvas.drawRoundRect(
                        x,y,x+58,y+34,
                        7,7,paint
                    );

                    paint.setColor(
                        selected[n]?Color.WHITE:TEXT
                    );

                    paint.setTextSize(12);
                    paint.setTypeface(Typeface.DEFAULT_BOLD);

                    canvas.drawText(
                        String.format(Locale.US,"%02d",n),
                        x+17,y+22,paint
                    );
                }
            }

            paint.setColor(DARK);
            paint.setTextSize(18);
            paint.setTypeface(Typeface.DEFAULT_BOLD);

            canvas.drawText(
                "JOGO FINAL: "+
                DiaDeSorteEngine.join(current.game),
                30,390,paint
            );

            canvas.drawText(
                "MÊS DA SORTE: "+
                DiaDeSorteEngine.monthName(current.month),
                30,420,paint
            );

            paint.setTextSize(13);
            canvas.drawText("RESUMO DO ESTUDO",30,455,paint);

            paint.setColor(TEXT);
            paint.setTextSize(9);
            paint.setTypeface(Typeface.DEFAULT);

            drawWrapped(
                canvas,
                current.detail,
                30,475,
                535,12,24,
                paint
            );

            document.finishPage(page);

            String fileName=
                "DIA_DE_SORTE_"+
                System.currentTimeMillis()+
                ".pdf";

            OutputStream out;

            if(Build.VERSION.SDK_INT>=29){
                ContentValues values=new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME,fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE,"application/pdf");
                values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "Download/DIA_DE_SORTE_PDF"
                );

                Uri outUri=getContentResolver().insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                );

                if(outUri==null){
                    throw new IOException("Não foi possível criar o PDF.");
                }

                out=getContentResolver().openOutputStream(outUri);
            }else{
                File dir=new File(
                    getExternalFilesDir(null),
                    "DIA_DE_SORTE_PDF"
                );

                if(!dir.exists()) dir.mkdirs();

                out=new FileOutputStream(
                    new File(dir,fileName)
                );
            }

            if(out==null){
                throw new IOException("Saída do PDF indisponível.");
            }

            document.writeTo(out);
            out.close();
            document.close();

            toast("PDF salvo em Downloads/DIA_DE_SORTE_PDF");
            status.setText("PDF salvo: "+fileName);

        }catch(Exception e){
            status.setText("Erro PDF: "+e.getMessage());
        }
    }

    void drawWrapped(
        Canvas canvas,
        String text,
        float x,float y,
        float maxWidth,
        float lineHeight,
        int maxLines,
        Paint paint
    ){
        ArrayList<String> lines=new ArrayList<>();

        for(String paragraph:text.replace("\r","").split("\n")){
            if(paragraph.trim().isEmpty()){
                lines.add("");
                continue;
            }

            String line="";

            for(String word:paragraph.split("\\s+")){
                String test=
                    line.isEmpty()
                    ?word
                    :line+" "+word;

                if(paint.measureText(test)>maxWidth){
                    if(!line.isEmpty()) lines.add(line);
                    line=word;
                }else{
                    line=test;
                }
            }

            if(!line.isEmpty()) lines.add(line);
        }

        for(int i=0;i<Math.min(maxLines,lines.size());i++){
            canvas.drawText(
                lines.get(i),
                x,
                y+i*lineHeight,
                paint
            );
        }
    }

    LinearLayout column(){
        LinearLayout layout=new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    TextView label(String text,int size,boolean bold,int color){
        TextView view=new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        if(bold){
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }

        return view;
    }

    Button button(String text){
        Button button=new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(BROWN);
        button.setAllCaps(false);
        return button;
    }

    void title(String text){
        TextView view=label(text,21,true,TEXT);
        view.setPadding(0,dp(16),0,dp(7));
        root.addView(view);
    }

    int dp(int n){
        return (int)(
            n*
            getResources()
            .getDisplayMetrics()
            .density+
            0.5f
        );
    }

    void toast(String text){
        Toast.makeText(
            this,
            text,
            Toast.LENGTH_SHORT
        ).show();
    }
}
