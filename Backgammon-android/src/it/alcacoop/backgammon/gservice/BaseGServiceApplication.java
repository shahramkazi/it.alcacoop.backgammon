/**
 ##################################################################
 #                       BACKGAMMON MOBILE                        #
 ##################################################################
 #                                                                #
 #  Authors: Domenico Martella - Francesco Valente                #
 #  E-mail: info@alcacoop.it                                      #
 #  Date:   18/10/2013                                            #
 #                                                                #
 ##################################################################
 #                                                                #
 #  Copyright (C) 2013   Alca Societa' Cooperativa                #
 #                                                                #
 #  This file is part of BACKGAMMON MOBILE.                       #
 #  FOUR IN A LINE MOBILE is free software: you can redistribute  # 
 #  it and/or modify it under the terms of the GNU General        #
 #  Public License as published by the Free Software Foundation,  #
 #  either version 3 of the License, or (at your option)          #
 #  any later version.                                            #
 #                                                                #
 #  FOUR IN A LINE MOBILE is distributed in the hope that it      #
 #  will be useful, but WITHOUT ANY WARRANTY; without even the    #
 #  implied warranty of MERCHANTABILITY or FITNESS FOR A          #
 #  PARTICULAR PURPOSE.  See the GNU General Public License       #
 #  for more details.                                             #
 #                                                                #
 #  You should have received a copy of the GNU General            #
 #  Public License v3 along with this program.                    #
 #  If not, see <http://http://www.gnu.org/licenses/>             #
 #                                                                #
 ##################################################################
 **/

package it.alcacoop.backgammon.gservice;

import it.alcacoop.backgammon.GnuBackgammon;
import it.alcacoop.backgammon.R;
import it.alcacoop.backgammon.fsm.BaseFSM.Events;
import it.alcacoop.backgammon.fsm.MenuFSM;
import it.alcacoop.backgammon.ui.UIDialog;
import it.alcacoop.backgammon.utils.AchievementsManager;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.google.android.gms.appstate.AppStateManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.leaderboard.Leaderboards.SubmitScoreResult;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMultiplayer;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;

@SuppressLint("InflateParams")
public abstract class BaseGServiceApplication extends AndroidApplication
    implements GServiceGameHelper.GameHelperListener, RealTimeMessageReceivedListener, RoomStatusUpdateListener,
    RoomUpdateListener, OnInvitationReceivedListener, RealTimeMultiplayer.ReliableMessageSentCallback {

  protected Preferences prefs;
  protected GServiceGameHelper gHelper;
  protected static int APP_DATA_KEY = 0;
  private boolean gConnecting = false;
  private boolean gServiceGameCanceled = false;
  private AlertDialog invitationDialog;
  private SecureRandom rnd;

  protected String mRoomId = null;
  protected String mMyId = null;
  protected ArrayList<Participant> mParticipants = null;
  protected boolean meSentInvitation;

  ProgressDialog mProgressDialog = null;
  protected static int RC_SELECT_PLAYERS = 6000;
  protected static int RC_WAITING_ROOM = 6001;
  protected static int RC_LEADERBOARD = 6002;
  protected static int RC_ACHIEVEMENTS = 6003;

  protected static int FROM_ACHIEVEMENTS = 1;
  protected static int FROM_SCOREBOARDS = 2;

  protected String invitationId = "";

  protected abstract boolean shouldShowInvitationDialog();
  protected abstract void onRoomConnectedBehaviour();
  protected abstract void onLeaveRoomBehaviour(int reason);
  protected abstract void onLeftRoomBehaviour();
  protected abstract void onRTMessageReceivedBehaviour(String msg);
  protected abstract void onErrorBehaviour(String msg);
  protected abstract void onStateLoadedBehaviour(byte[] data);
  protected abstract byte[] onStateConflictBehaviour(byte[] localData, byte[] serverData);
  protected abstract void onResetRoomBehaviour();
  protected abstract void onScoreSubmittedBehaviour(String board_id, SubmitScoreResult arg1);
  protected abstract void onDismissProgressDialogBehaviour();


  @SuppressLint("TrulyRandom")
  public BaseGServiceApplication() {
    try {
      rnd = SecureRandom.getInstance("SUN");
    } catch (NoSuchAlgorithmException e) {
      rnd = new SecureRandom();
    }
  }


  private void onStateConflict(AppStateManager.StateConflictResult conflictResult) {
    AppStateManager.resolve(getApiClient(), conflictResult.getStateKey(), conflictResult.getResolvedVersion(),
        onStateConflictBehaviour(conflictResult.getLocalData(), conflictResult.getServerData()));
  }
  private void onStateLoaded(Status status, int stateKey, byte[] data) {
    if (status.isSuccess()) {
      onStateLoadedBehaviour(data);
    }
  }


  @Override
  public void onRealTimeMessageSent(int statusCode, int token, String recipientParticipantId) {
    System.out.println("==> SENT.. " + statusCode + " : " + recipientParticipantId);

    if (statusCode != GServiceClient.STATUS_OK) {
      onLeaveRoomBehaviour(GServiceClient.STATUS_NETWORK_ERROR_OPERATION_FAILED);
      GServiceClient.getInstance().leaveRoom(GServiceClient.STATUS_NETWORK_ERROR_OPERATION_FAILED);
    }
  }

  @Override
  public void onInvitationReceived(Invitation invitation) {
    System.out.println("==> INVITATION RECEIVED");
    if (!shouldShowInvitationDialog()) {
      Games.RealTimeMultiplayer.declineInvitation(getApiClient(), invitation.getInvitationId());
      return;
    }
    gserviceInvitationReceived(invitation.getInviter().getIconImageUri(), invitation.getInviter().getDisplayName(), invitation.getInvitationId());
  }

  @Override
  public void onInvitationRemoved(String arg0) {
    if (invitationDialog != null)
      invitationDialog.dismiss();
    hideProgressDialog();
    onErrorBehaviour("Opponent canceled invitation");
  }

  @Override
  public void onJoinedRoom(int arg0, Room room) {
    System.out.println("---> JOIN ROOM");
    if (room == null) {
      System.out.println("---> 2 LEFT ROOM");
      hideProgressDialog();
      onErrorBehaviour("Invalid invitation");
    } else {
      updateRoom(room);
      gConnecting = true;
    }
  }

  @Override
  public void onLeftRoom(int arg0, String arg1) {
    System.out.println("---> P2P LEFT ROOM");
    onLeftRoomBehaviour();
  }

  @Override
  public void onRoomConnected(int arg0, Room room) {
    System.out.println("---> GSERVICE onRoomConnected");
    hideProgressDialog();
    updateRoom(room);
    onRoomConnectedBehaviour();
    gConnecting = false;
    showProgressDialog(true);
  }

  @Override
  public void onRoomCreated(int statusCode, Room room) {
    if (statusCode != GServiceClient.STATUS_OK) {
      hideProgressDialog();
      onErrorBehaviour("Unknown error");
      return;
    }
    mRoomId = room.getRoomId();
    meSentInvitation = true;
    Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(getApiClient(), room, Integer.MAX_VALUE);
    startActivityForResult(i, RC_WAITING_ROOM);
  }


  @Override
  public void onConnectedToRoom(Room room) {
    if (gServiceGameCanceled) {
      gServiceGameCanceled = false;
      Games.RealTimeMultiplayer.leave(getApiClient(), this, room.getRoomId());
    }
    GServiceClient.getInstance().reset();

    mParticipants = room.getParticipants();
    room.getParticipantId(Games.Players.getCurrentPlayerId(getApiClient()));
    mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(getApiClient()));
    updateRoom(room);
    String opponent_player_id;

    String sRdm = new BigInteger(130, rnd).toString(32);

    if (mParticipants.get(0).getParticipantId() == mMyId) {
      if (mParticipants.get(1).getPlayer() == null)
        opponent_player_id = sRdm;
      else
        opponent_player_id = mParticipants.get(1).getPlayer().getPlayerId();
    } else {
      if (mParticipants.get(0).getPlayer() == null)
        opponent_player_id = sRdm;
      else
        opponent_player_id = mParticipants.get(0).getPlayer().getPlayerId();
    }
    if (meSentInvitation)
      AchievementsManager.getInstance().checkSocialAchievements(opponent_player_id);
  }
  @Override
  public void onDisconnectedFromRoom(Room room) {
    System.out.println("---> P2P DISCONNECTED FROM ROOM");
  }

  @Override
  public void onPeerDeclined(Room room, List<String> arg1) {
    updateRoom(room);
  }

  @Override
  public void onPeerInvitedToRoom(Room room, List<String> arg1) {
    updateRoom(room);
  }

  @Override
  public void onPeerJoined(Room room, List<String> arg1) {
    updateRoom(room);
  }

  @Override
  public void onPeerLeft(Room room, List<String> arg1) {
    System.out.println("---> P2P PEER LEFT");
    if (gConnecting) {
      hideProgressDialog();
      _gserviceResetRoom();
      onErrorBehaviour("Error: peer left the room");
      updateRoom(room);
    }
  }

  @Override
  public void onPeersConnected(Room room, List<String> arg1) {
    updateRoom(room);
  }

  @Override
  public void onPeersDisconnected(Room room, List<String> arg1) {
    System.out.println("---> P2P PEER DISCONNECTED");
    GServiceClient.getInstance().leaveRoom(0);
    onLeftRoomBehaviour();
    updateRoom(room);
  }

  @Override
  public void onRoomAutoMatching(Room room) {
    updateRoom(room);
  }

  @Override
  public void onRoomConnecting(Room room) {
    updateRoom(room);
  }

  @Override
  public void onRealTimeMessageReceived(RealTimeMessage rtm) {
    byte[] buf = rtm.getMessageData();
    String s = new String(buf);
    System.out.print("==> RECEIVED.. " + s);
    onRTMessageReceivedBehaviour(s);
  }

  @Override
  public void onSignInFailed() {}

  @Override
  public void onSignInSucceeded() {
    System.out.println("---> SIGNIN SUCCEDED!");
    prefs.putBoolean("ALREADY_SIGNEDIN", true);
    prefs.flush();
    // TODO
    Games.Invitations.registerInvitationListener(getApiClient(), this);

    AppStateManager.load(getApiClient(), APP_DATA_KEY).setResultCallback(
        new ResultCallback<AppStateManager.StateResult>() {
          @Override
          public void onResult(AppStateManager.StateResult result) {
            AppStateManager.StateConflictResult conflictResult = result.getConflictResult();
            AppStateManager.StateLoadedResult loadedResult = result.getLoadedResult();
            if (loadedResult != null) {
              onStateLoaded(loadedResult.getStatus(), loadedResult.getStateKey(), loadedResult.getLocalData());
            } else if (conflictResult != null) {
              onStateConflict(conflictResult);
            }
          }
        });

    if (gHelper.getInvitationId() != null && gHelper.isSignedIn()) {
      invitationId = gHelper.getInvitationId();
      System.out.println("---> INVITE FROM NOTIFICATION AREA " + invitationId);
      GnuBackgammon.Instance.invitationId = invitationId;
    }
  }
  void gserviceInvitationReceived(final Uri imagesrc, final String username, final String invitationId) {
    final AlertDialog.Builder alert = new AlertDialog.Builder(this);
    final LayoutInflater inflater = this.getLayoutInflater();
    GnuBackgammon.fsm.state(MenuFSM.States.TWO_PLAYERS);

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final View myView = inflater.inflate(R.layout.dialog_invitation, null);
        alert.setView(myView).setTitle("Invitation received").setCancelable(false).setNegativeButton("Decline", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Games.RealTimeMultiplayer.declineInvitation(getApiClient(), invitationId);
          }
        });
        alert.setPositiveButton("Accept", null);

        invitationDialog = alert.create();
        invitationDialog.setOnShowListener(new DialogInterface.OnShowListener() {
          @Override
          public void onShow(DialogInterface arg0) {
            ImageManager im = ImageManager.create(getApplicationContext());
            im.loadImage(((ImageView)myView.findViewById(R.id.image)), imagesrc);
            TextView tv = (TextView)myView.findViewById(R.id.text);
            tv.setText(username + " wants to play with you...");
            tv.setFocusable(true);
            tv.setFocusableInTouchMode(true);
            tv.requestFocus();
            Button b = invitationDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                _gserviceAcceptInvitation(invitationId);
                invitationDialog.dismiss();
              }
            });
          }
        });
        invitationDialog.show();
      }
    });
  }

  public void _gserviceAcceptInvitation(String invitationId) {
    RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
    roomConfigBuilder.setInvitationIdToAccept(invitationId);
    roomConfigBuilder.setMessageReceivedListener(this);
    roomConfigBuilder.setRoomStatusUpdateListener(this);
    _gserviceResetRoom();
    Games.RealTimeMultiplayer.join(getApiClient(), roomConfigBuilder.build());
    showProgressDialog();
  }

  private void updateRoom(Room room) {
    System.out.println("---> P2P UPDATE ROOM " + room.getRoomId());
    if (room != null) {
      mRoomId = room.getRoomId();
      mParticipants = room.getParticipants();
    }
  }

  public void showProgressDialog() {
    showProgressDialog(false);
  }

  public void showProgressDialog(final boolean cancel_button) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mProgressDialog == null) {
          if (BaseGServiceApplication.this == null)
            return;
          mProgressDialog = new ProgressDialog(BaseGServiceApplication.this);
        }
        mProgressDialog.setMessage("Please wait..");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);

        if (cancel_button) {
          mProgressDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              gServiceGameCanceled = true;
              mProgressDialog.dismiss();
              onDismissProgressDialogBehaviour();
            }
          });
        }
        mProgressDialog.show();
      }
    });
  }

  public void hideProgressDialog() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mProgressDialog != null) {
          mProgressDialog.dismiss();
          mProgressDialog = null;
        }
      }
    });
  }

  @Override
  public void onP2PConnected(String arg0) {
    System.out.println("---> P2P CONNECTED");
  }

  @Override
  public void onP2PDisconnected(String arg0) {
    System.out.println("---> P2P DISCONNECTED");
  }


  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);
    prefs = Gdx.app.getPreferences("GameOptions");
    gHelper = new GServiceGameHelper(this, GServiceGameHelper.CLIENT_APPSTATE | GServiceGameHelper.CLIENT_GAMES);
    gHelper.setup(this);
    gHelper.enableDebugLog(true);
  }

  @Override
  protected void onStart() {
    super.onStart();
    gHelper.onStart(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mRoomId != null) {
      onLeftRoomBehaviour();
    }
    gHelper.onStop();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    gHelper.onActivityResult(requestCode, resultCode, data);
    if (requestCode == RC_SELECT_PLAYERS) {
      if (resultCode == RESULT_OK) {
        showProgressDialog();
        Bundle autoMatchCriteria = null;
        int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
        if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
          autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0);
        }
        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

        // create the room
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.addPlayersToInvite(invitees);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        if (autoMatchCriteria != null) {
          rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        }
        Games.RealTimeMultiplayer.create(getApiClient(), rtmConfigBuilder.build());
      }
    } else if (requestCode == RC_WAITING_ROOM) {
      if (resultCode != RESULT_OK)
        _gserviceResetRoom();
      else {
        if (mRoomId != null) {
          System.out.println("---> FORSE LO PRENDO DI QUI???");
          showProgressDialog(true);
        } else {
          UIDialog.getFlashDialog(Events.NOOP, "Opponent abandoned game");
        }
      }
    }
  }

  public void _gserviceResetRoom() {
    onResetRoomBehaviour();
    gConnecting = false;
    meSentInvitation = false;
    if (mRoomId != null) {
      Games.RealTimeMultiplayer.leave(getApiClient(), this, mRoomId);
      mRoomId = null;
    }
    gServiceGameCanceled = false;
  }

  protected void _gserviceSignIn() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        gHelper.beginUserInitiatedSignIn();
      }
    });
  }

  public void gserviceGetSigninDialog(final int from) {
    final AlertDialog.Builder alert = new AlertDialog.Builder(this);
    final LayoutInflater inflater = this.getLayoutInflater();

    runOnUiThread(new Runnable() {
      @SuppressLint("NewApi")
      @Override
      public void run() {
        final View myView = inflater.inflate(R.layout.dialog_gplus, null);
        alert.setView(myView).setTitle("Signin").setCancelable(true);
        final AlertDialog d = alert.create();
        d.setOnShowListener(new DialogInterface.OnShowListener() {
          @Override
          public void onShow(DialogInterface arg0) {
            String msg = "";
            TextView v = (TextView)d.findViewById(R.id.login_text);
            if (prefs.getBoolean("ALREADY_SIGNEDIN", false)) {
              msg = "Please sign in on Google Play Game Services to enable this feature";
            } else {
              msg = "Please sign in, Google will ask you to accept requested permissions and configure " + "sharing settings up to two times. This may take few minutes..";
            }
            v.setText(msg);
            com.google.android.gms.common.SignInButton b = (com.google.android.gms.common.SignInButton)d.findViewById(R.id.sign_in_button);
            b.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                d.dismiss();
                trySignIn(from);
              }
            });
          }
        });
        d.show();
      }
    });
  }

  protected void trySignIn(final int from) {
    if ((from == FROM_ACHIEVEMENTS) || (from == FROM_SCOREBOARDS)) {
      gHelper.setListener(new GServiceGameHelper.GameHelperListener() {
        @Override
        public void onSignInSucceeded() {
          gHelper.setListener(this);
          if (from == FROM_ACHIEVEMENTS)
            startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()), RC_ACHIEVEMENTS);
          else
            startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(getApiClient()), RC_LEADERBOARD);
        }
        @Override
        public void onSignInFailed() {
          onErrorBehaviour("Login error");
        }
      });
    }
    _gserviceSignIn();
  }

  public GoogleApiClient getApiClient() {
    return gHelper.getApiClient();
  }
}