package ke.co.kbanda.tafuta.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import ke.co.kbanda.tafuta.R;
import ke.co.kbanda.tafuta.models.Member;

public class MembersListAdapter extends RecyclerView.Adapter<MembersListAdapter.MembersListViewHolder> {
    private List<Member> membersList;
    private Context context;
    private OnMemberClickedListener onMemberClickedListener;

    public interface OnMemberClickedListener {
        void onClick(int position);
    }

    public MembersListAdapter(List<Member> membersList, Context context) {
        this.membersList = membersList;
        this.context = context;
    }

    public void setOnMemberClickedListener(OnMemberClickedListener clickedListener) {
        this.onMemberClickedListener = clickedListener;
    }

    @NonNull
    @Override
    public MembersListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MembersListViewHolder(
                LayoutInflater
                        .from(context)
                        .inflate(R.layout.item_members, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull MembersListViewHolder holder, int position) {
        Member currentMember = membersList.get(position);

        holder.names.setText(currentMember.getFirstName() + " " + currentMember.getLastName());
        holder.email.setText(currentMember.getEmail());
        holder.label.setText(currentMember.getLabel());

        if (currentMember.getImageUrl() != null && !currentMember.getImageUrl().isEmpty()) {
            Glide
                    .with(context)
                    .load(currentMember.getImageUrl())
                    .centerCrop()
                    .into(holder.profileImage)
            ;
        }
    }

    @Override
    public int getItemCount() {
        return membersList.size();
    }

    class MembersListViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView profileImage;
        private final TextView names;
        private final TextView label;
        private final TextView email;

        public MembersListViewHolder(@NonNull View itemView) {
            super(itemView);

            profileImage = itemView.findViewById(R.id.profileImage);
            names = itemView.findViewById(R.id.names);
            label = itemView.findViewById(R.id.label);
            email = itemView.findViewById(R.id.email);

            itemView
                    .setOnClickListener(v -> {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            onMemberClickedListener.onClick(position);
                        }
                    })
            ;
        }
    }
}
