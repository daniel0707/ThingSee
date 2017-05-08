package daniel_vahlametropolia.thingsee;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class StatisticsActivity extends Activity implements View.OnClickListener{
    private Double TotalDistance;
    private Double TotalTime;
    private Double AverageSpeed;

    private TextView showDistance;
    private TextView showTime;
    private TextView showAveSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        TotalDistance = getIntent().getDoubleExtra("totalDistance",0);
        TotalTime = getIntent().getDoubleExtra("totalTime",0);
        AverageSpeed = getIntent().getDoubleExtra("aveSpeed",0);

        showDistance = (TextView)findViewById(R.id.showDistance);
        showTime = (TextView)findViewById(R.id.showTime);
        showAveSpeed = (TextView)findViewById(R.id.showAveSpeed);

        showDistance.setText(TotalDistance.toString());
        showTime.setText(TotalTime.toString());
        showAveSpeed.setText(AverageSpeed.toString());

        Button myButton = (Button)findViewById(R.id.back);
        myButton.setOnClickListener(this);
    }


    public void onClick(View v) {
        Intent res = new Intent();
        setResult(RESULT_OK,res);
        finish();
    }
}
