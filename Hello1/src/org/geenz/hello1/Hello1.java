package org.geenz.hello1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Hello1 extends Activity {
    protected static final int A_DIALOG = 0;
    protected static final int ANGRY_DIALOG = 1;
    
    private static int clickCounter = 0;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);  
        
        // Access xml defined view, programmatically update text        
        TextView textView = (TextView) findViewById(R.id.helloTextView);
        textView.setText(R.string.new_text);
        
        // Create new View programmatically, add to existing layout
        ImageButton button = new ImageButton(this);
        button.setImageResource(android.R.drawable.ic_popup_reminder); // system default resources
        
        LinearLayout layout = (LinearLayout) findViewById(R.id.helloLayout);
        layout.addView(button);
        
        button.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		if (clickCounter > 0) {
        			clickCounter = 0;
        			showDialog(ANGRY_DIALOG);        			
        		} else {
        			clickCounter = clickCounter + 1;        		
        			showDialog(A_DIALOG); // the easy way to show a dialog, work done in onCreateDialog
        		}
        			
        	}
        });
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	switch (id) {
    	case A_DIALOG:
    		/*
    		 * Creates a dialog, including a close button, using Dialog.Builder 
    		 */
    		return new AlertDialog.Builder(this)
    			.setTitle("Don't press me!")
    			.setIcon(android.R.drawable.ic_dialog_info)
    			.setMessage("You pressed me!")
    			.setPositiveButton("I won't do it again", new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int which) {
    					dialog.cancel();
    				}
    			})
    			.create();
    	case ANGRY_DIALOG:
    		return new AlertDialog.Builder(this)
			.setTitle("You Lied!")
			.setIcon(android.R.drawable.ic_dialog_info)
			.setMessage("I'm never going to talk to you again!")
			.setPositiveButton("Sorry!", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			})
			.create();
    	}
    	return null;
    }
}