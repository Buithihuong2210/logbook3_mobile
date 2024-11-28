package com.example.todolistapp

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var editTextTask: EditText
    private lateinit var buttonAdd: Button
    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var buttonFilterCompleted: Button
    private lateinit var buttonFilterIncomplete: Button
    private lateinit var buttonShowAll: Button
    private lateinit var editTextSearch: EditText
    private lateinit var buttonSearch: Button
    private val tasks = mutableListOf<Task>()
    private lateinit var adapter: TaskAdapter
    private var editedTaskPosition: Int? = null
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextTask = findViewById(R.id.editTextTask)
        buttonAdd = findViewById(R.id.buttonAdd)
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        buttonFilterCompleted = findViewById(R.id.buttonFilterCompleted)
        buttonFilterIncomplete = findViewById(R.id.buttonFilterIncomplete)
        buttonShowAll = findViewById(R.id.buttonShowAll)
        editTextSearch = findViewById(R.id.editTextSearch)
        buttonSearch = findViewById(R.id.buttonSearch)

        dbHelper = DatabaseHelper(this)
        tasks.addAll(dbHelper.getAllTasks())

        adapter = TaskAdapter(tasks)
        recyclerViewTasks.adapter = adapter
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)

        buttonAdd.setOnClickListener {
            addOrUpdateTask()
        }
        editTextTask.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                addOrUpdateTask()
                true
            } else { false }
        }
        buttonSearch.setOnClickListener {
            val query = editTextSearch.text.toString().trim()
            searchTasks(query)
        }
        editTextSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val query = editTextSearch.text.toString().trim()
                searchTasks(query)
                true
            } else { false }
        }
        buttonFilterCompleted.setOnClickListener {
            filterTasks(isCompleted = true)
        }
        buttonFilterIncomplete.setOnClickListener {
            filterTasks(isCompleted = false)
        }
        buttonShowAll.setOnClickListener {
            showAllTasks()
        }
    }

    private fun searchTasks(query: String) {
        if (query.isEmpty()) {
            showAllTasks()
        } else {
            val searchResults = dbHelper.getAllTasks().filter {
                it.name.contains(query, ignoreCase = true)
            }

            tasks.clear()
            tasks.addAll(searchResults)

            adapter.notifyDataSetChanged()

            if (searchResults.isEmpty()) {
                Toast.makeText(this, "No tasks found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to add or update a task
    private fun addOrUpdateTask() {
        val taskName = editTextTask.text.toString().trim()
        editTextTask.error = null

        if (taskName.isEmpty()) {
            editTextTask.error = "Task cannot be empty!"
            return
        }

        if (editedTaskPosition != null) {
            val taskToEdit = tasks[editedTaskPosition!!]
            val updatedTask = Task(taskToEdit.id, taskName, taskToEdit.isCompleted)
            dbHelper.updateTask(updatedTask)
            tasks[editedTaskPosition!!] = updatedTask
            editedTaskPosition = null
            buttonAdd.text = "Add Task"
        } else {
            val newTask = Task(name = taskName, isCompleted = false)
            val newTaskId = dbHelper.addTask(newTask)
            tasks.add(Task(newTaskId.toInt(), taskName, false))
        }

        adapter.notifyDataSetChanged()
        editTextTask.text.clear()
    }

    private fun filterTasks(isCompleted: Boolean) {
        val filteredTasks = dbHelper.getAllTasks().filter { it.isCompleted == isCompleted }
        tasks.clear()
        tasks.addAll(filteredTasks)
        adapter.notifyDataSetChanged()
    }

    private fun showAllTasks() {
        tasks.clear()
        tasks.addAll(dbHelper.getAllTasks())
        adapter.notifyDataSetChanged()
    }


    private fun editTask(position: Int) {
        editTextTask.setText(tasks[position].name)
        editedTaskPosition = position
        buttonAdd.text = "Update Task"
    }

    private fun deleteTask(position: Int) {
        val taskToDelete = tasks[position]

        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Yes") { _, _ ->
                dbHelper.deleteTask(taskToDelete.id!!)
                tasks.removeAt(position)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("No", null)
            .show()
    }

    inner class TaskAdapter(private val tasks: MutableList<Task>) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
        inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textViewTask: TextView = itemView.findViewById(R.id.textViewTask)
            val checkBoxComplete: CheckBox = itemView.findViewById(R.id.checkBoxComplete)
            val buttonEdit: Button = itemView.findViewById(R.id.buttonEdit)
            val buttonDelete: Button = itemView.findViewById(R.id.buttonDelete)

            init {
                buttonEdit.setOnClickListener {
                    editTask(adapterPosition)
                }

                buttonDelete.setOnClickListener {
                    deleteTask(adapterPosition)
                }

                checkBoxComplete.setOnCheckedChangeListener(null)
                checkBoxComplete.setOnCheckedChangeListener { _, isChecked ->
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        tasks[position].isCompleted = isChecked
                        dbHelper.updateTask(tasks[position])
                        notifyItemChanged(position)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
            return TaskViewHolder(view)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val task = tasks[position]

            holder.checkBoxComplete.setOnCheckedChangeListener(null)
            holder.checkBoxComplete.isChecked = task.isCompleted
            holder.textViewTask.text = task.name
            holder.textViewTask.paint.isStrikeThruText = task.isCompleted

            holder.checkBoxComplete.setOnCheckedChangeListener { _, isChecked ->
                task.isCompleted = isChecked
                dbHelper.updateTask(task)
                notifyItemChanged(position)
            }
        }

        override fun getItemCount(): Int {
            return tasks.size
        }
    }
}
