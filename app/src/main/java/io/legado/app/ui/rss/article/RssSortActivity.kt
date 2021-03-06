package io.legado.app.ui.rss.article

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.utils.getViewModel
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.activity_rss_artivles.*
import org.jetbrains.anko.startActivityForResult
import java.util.*

class RssSortActivity : VMBaseActivity<RssSortViewModel>(R.layout.activity_rss_artivles) {

    override val viewModel: RssSortViewModel
        get() = getViewModel(RssSortViewModel::class.java)
    private val editSource = 12319
    private val fragments = linkedMapOf<String, RssArticlesFragment>()
    private lateinit var adapter: TabFragmentPageAdapter
    private val channels = LinkedHashMap<String, String>()
    private var groupMenu: Menu? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        adapter = TabFragmentPageAdapter(supportFragmentManager)
        tab_layout.setupWithViewPager(view_pager)
        view_pager.adapter = adapter
        viewModel.titleLiveData.observe(this, Observer {
            title_bar.title = it
        })
        viewModel.initData(intent) {
            upChannelMenu()
            upFragments()
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_articles, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        groupMenu = menu
        upChannelMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit_source -> viewModel.rssSource?.sourceUrl?.let {
                startActivityForResult<RssSourceEditActivity>(editSource, Pair("data", it))
            }
            R.id.menu_clear -> {
                viewModel.url?.let {
                    viewModel.clearArticles()
                }
            }
        }
        if (item.groupId == R.id.source_channel) {
            val key = item.title.toString();
            val i = fragments.keys.indexOf(key)
            if (i >= 0)
                view_pager.currentItem = i
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun upChannelMenu() {
        // 加入频道列表
        groupMenu?.removeGroup(R.id.source_channel)
        val sourceChannel = viewModel.rssSource?.sourceGroup
        channels.clear()
        sourceChannel?.split("\n\n")?.forEach { c ->
            val d = c.split("::")
            if (d.size > 1) {
                channels[d[0]] = d[1]
                val item = groupMenu?.add(R.id.source_channel, Menu.NONE, Menu.NONE, d[0])
                item?.isCheckable = true
                val keys = fragments.keys
                item?.isChecked = keys.indexOf(d[0]) == view_pager.currentItem
            }
        }
    }

    private fun upFragments() {
        fragments.clear()
        viewModel.rssSource?.sortUrls()?.forEach {
            fragments[it.key] = RssArticlesFragment.create(it.key, it.value)
        }
        val sortUrlsSize = fragments.size
        if (sortUrlsSize <= 1) {
            channels.forEach {
                fragments[it.key] = RssArticlesFragment.create(it.key, it.value)
            }
        }
        if (sortUrlsSize == 1) {
            tab_layout.gone()
        } else {
            tab_layout.visible()
        }
        adapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            editSource -> if (resultCode == Activity.RESULT_OK) {
                viewModel.initData(intent) {
                    upFragments()
                }
            }
        }
    }

    private inner class TabFragmentPageAdapter internal constructor(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getPageTitle(position: Int): CharSequence? {
            return fragments.keys.elementAt(position)
        }

        override fun getItem(position: Int): Fragment {
            return fragments.values.elementAt(position)
        }

        override fun getCount(): Int {
            return fragments.size
        }
    }

}