package com.absinthe.libchecker.features.about;

import static android.net.Uri.parse;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drakeet.about.Contributor;
import com.drakeet.about.OnContributorClickedListener;
import com.drakeet.about.R;
import com.drakeet.multitype.ItemViewBinder;

/**
 * @author drakeet
 */
@SuppressWarnings("WeakerAccess")
public class ContributorViewBinder extends ItemViewBinder<Contributor, ContributorViewBinder.ViewHolder> {

  private @NonNull final AbsAboutActivityProxy activity;

  public ContributorViewBinder(@NonNull AbsAboutActivityProxy activity) {
    this.activity = activity;
  }

  @Override @NonNull
  public ContributorViewBinder.ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
    return new ContributorViewBinder.ViewHolder(inflater.inflate(R.layout.about_page_item_contributor, parent, false), activity);
  }

  @Override
  public void onBindViewHolder(@NonNull ContributorViewBinder.ViewHolder holder, @NonNull Contributor contributor) {
    holder.avatar.setImageResource(contributor.avatarResId);
    holder.name.setText(contributor.name);
    holder.desc.setText(contributor.desc);
    holder.data = contributor;
  }

  @Override
  public long getItemId(@NonNull Contributor item) {
    return item.hashCode();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public final ImageView avatar;
    public final TextView name;
    public final TextView desc;
    public Contributor data;
    protected @NonNull
    final AbsAboutActivityProxy activity;

    public ViewHolder(View itemView, @NonNull AbsAboutActivityProxy activity) {
      super(itemView);
      this.activity = activity;
      avatar = itemView.findViewById(R.id.avatar);
      name = itemView.findViewById(R.id.name);
      desc = itemView.findViewById(R.id.desc);
      itemView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
      OnContributorClickedListener listener = activity.getOnContributorClickedListener();
      if (listener != null && listener.onContributorClicked(v, data)) {
        return;
      }
      if (data.url != null) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(parse(data.url));
        try {
          v.getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
