package com.example.nawigacja_wewnatrz_budynku.otherScripts

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nawigacja_wewnatrz_budynku.R
import com.example.nawigacja_wewnatrz_budynku.dataClasses.AdminPageItems

class RecyclerViewAdapter(private val items: List<AdminPageItems>) : RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder>(){

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.iconAdminPage)
        private val textView: TextView = view.findViewById(R.id.labelAdminPage)
        private val itemLayout: RelativeLayout = view.findViewById(R.id.itemAdminPage)

        fun bind(item: AdminPageItems) {
            imageView.setImageResource(item.iconResId)
            textView.text = item.label
            itemLayout.setOnClickListener{
                item.callback.invoke()
            }
        }
    }
    class HorizontalSpaceItemDecoration(private val horizontalSpaceWidth: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.right = horizontalSpaceWidth/2
            outRect.left = horizontalSpaceWidth/2
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_page, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}