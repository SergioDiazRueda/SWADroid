package es.ugr.swad.swadroid.modules.messages;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.ksoap2.serialization.SoapObject;
import java.util.ArrayList;
import java.util.Vector;
import es.ugr.swad.swadroid.Constants;
import es.ugr.swad.swadroid.R;
import es.ugr.swad.swadroid.model.User;
import es.ugr.swad.swadroid.model.UserFilter;
import es.ugr.swad.swadroid.modules.Module;
import es.ugr.swad.swadroid.modules.courses.Courses;
import es.ugr.swad.swadroid.modules.login.Login;
import es.ugr.swad.swadroid.webservices.SOAPClient;

/**
 * Created by Romilgildo on 17/07/2016.
 */
public class SearchUsers extends Module implements SearchView.OnQueryTextListener {

    private static final String TAG = Constants.APP_TAG + " SearchUsers";

    private SearchView searchView;
    private MenuItem searchItem;
    private static ListView lvUsers;
    private String[] receivers = {};
    private String search;
    private UsersAdapter adapter;
    private CheckBox checkbox;
    private UsersList userFilters = new UsersList();
    private LinearLayout progressLayout;

    private long courseCode;
    private int numUsers;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_items);
        setTitle(R.string.actionBarAddUser);

        //users list
        lvUsers = (ListView) findViewById(R.id.listItems);
        lvUsers.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        //checkbox is checked when the row of an user is clicked
        lvUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                checkbox = (CheckBox) view.findViewById(R.id.check);
                if (checkbox.isChecked())
                    checkbox.setChecked(false);
                else
                    checkbox.setChecked(true);

                //String idUser = userFilters.getUsers().get(position).getUserNickname();
            }
        });

        progressLayout = (LinearLayout) findViewById(R.id.progressbar_view);
        TextView textLoading = (TextView) findViewById(R.id.text_progress);
        textLoading.setText(R.string.loadingMsg);

        setMETHOD_NAME("findUsers");
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_users_bar, menu);

        searchItem = menu.findItem(R.id.action_search_field);
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener(){
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // Do whatever you need
                return true; // KEEP IT TO TRUE OR IT DOESN'T OPEN !!
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                invalidateOptionsMenu();
                setResult(RESULT_OK);
                finish();
                return true; // OR FALSE IF YOU DIDN'T WANT IT TO CLOSE!
            }
        });

        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        searchView.setQueryHint(getText(R.string.search_user));
        // listener to searchview
        searchView.setOnQueryTextListener(this);

        // searview expanded
        searchItem.expandActionView();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                if(!search.equals(""))
                onQueryTextSubmit(search);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean onQueryTextSubmit(String query) {
        if (Courses.getSelectedCourseCode() != -1){
            showDialogSearch();
        }
        else{
            courseCode = -1;
            runConnection();
        }

        //remove virtual keyboard
        //InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);

        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        search = newText;

        return true;
    }

    @Override
    protected void requestService() throws Exception {
        createRequest(SOAPClient.CLIENT_TYPE);
        addParam("wsKey", Login.getLoggedUser().getWsKey());
        addParam("courseCode", courseCode);
        addParam("filter", search);
        addParam("userRole", 0);
        sendRequest(User.class, false);

        if (result != null) {
            ArrayList<?> res = new ArrayList<Object>((Vector<?>) result);
            SoapObject soap = (SoapObject) res.get(1);
            int csSize = soap.getPropertyCount();
            numUsers = csSize;
            userFilters = new UsersList();

            for (int i = 0; i < csSize; i++) {
                SoapObject pii = (SoapObject) soap.getProperty(i);
                String nickname = pii.getProperty("userNickname").toString();
                String surname1 = pii.getProperty("userSurname1").toString();
                String surname2 = pii.getProperty("userSurname2").toString();
                String firstname = pii.getProperty("userFirstname").toString();
                String userPhoto = pii.getProperty("userPhoto").toString();
                Log.d(TAG, nickname + " " + surname1 + " " + surname2 + " " + firstname + " " + userPhoto);

                userFilters.saveUser(new UserFilter(nickname, surname1, surname2, firstname, userPhoto));
            }

            Log.d(TAG, "numUsers = " + String.valueOf(numUsers) + ", usersList = " + userFilters.getUsers().size());
        }

        setResult(RESULT_OK);
    }

    @Override
    protected void connect() {
        startConnection();
        searchView.clearFocus();

        progressLayout.setVisibility(View.VISIBLE);
    }

    @Override
    protected void postConnect() {
        progressLayout.setVisibility(View.GONE);
        adapter = new UsersAdapter(getBaseContext(), userFilters.getUsers());
        lvUsers.setAdapter(adapter);

        //message about found users
        Toast.makeText(SearchUsers.this, String.valueOf(numUsers) + " " + getResources().getString(R.string.users_found), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onError() {

    }

    private void showDialogSearch(){
        final String[] choiceList = {getString(R.string.in_subject) + " " + Courses.getSelectedCourseShortName(), getString(R.string.inAllPlatform)};

        AlertDialog.Builder builder = new AlertDialog.Builder(SearchUsers.this);
        builder.setTitle(R.string.where_to_search);
        builder.setCancelable(false);

        builder.setSingleChoiceItems(choiceList, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
            }
        });

        builder.setNegativeButton(getString(R.string.cancelMsg), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // no need to write anything here just implement this interface into this button
            }
        });

        builder.setPositiveButton(getString(R.string.acceptMsg), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                if (selectedPosition == 0)
                    courseCode = Courses.getSelectedCourseCode();
                else
                    courseCode = -1;
                runConnection();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
}
