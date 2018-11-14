package com.theevilroot.uadaquoter.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.theevilroot.uadaquoter.objects.License

class LicensesAdapter: RecyclerView.Adapter<LicensesAdapter.LicenseHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LicenseHolder {

    }


    override fun getItemCount(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBindViewHolder(holder: LicenseHolder, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    class LicenseHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        fun bind(license: License) {

        }

    }

}