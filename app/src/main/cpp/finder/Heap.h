#include <vector>
#include <stdexcept>


template<typename T>
class Heap {

private:
    std::vector<T *> data;

    // private utility functions
    void heapify();

    void bubble_up(int);

    void bubble_down(int);

    void swap_val(int, int);

public:
    // constructor
    Heap();

    // public member functions
    Heap &push(T *t);

    Heap &pop();

    T *peek() const;

    void clear();

    inline int size() const { return (int) data.size(); }

    inline bool empty() const { return data.empty(); }
};


// constructors
template<typename T>
Heap<T>::Heap() : data() { }

// add a new element to Heap
template<typename T>
Heap<T> &Heap<T>::push(T *obj) {
    data.push_back(obj);
    int last_index = size() - 1;
    bubble_up(last_index);
    return *this;
}

// remove the minimum element from the Heap
template<typename T>
Heap<T> &Heap<T>::pop() {
    if (!empty()) {
        int last_index = size() - 1;
        swap_val(0, last_index);
        data.pop_back();
        bubble_down(0);
    }
    return *this;
}

// return the minimum value
template<typename T>
T *Heap<T>::peek() const {
    if (empty()) { throw std::out_of_range("empty heap!"); }
    return data[0];
}


// private utility functions


// heapify an ordinary array
template<typename T>
void Heap<T>::heapify() {
    if (empty()) { return; }
    int last_index = size() - 1;
    for (int i = last_index; i >= 0; --i) { bubble_down(i); }
}

// bubble_up operation
template<typename T>
void Heap<T>::bubble_up(int index) {
    if (index == 0) { return; }
    int parent_index = static_cast<int>((index - 1) / 2);
    if (*(data[parent_index]) > *(data[index])) {
        swap_val(index, parent_index);
        bubble_up(parent_index);
    }
    return;
}

// bubble_down operation
template<typename T>
void Heap<T>::bubble_down(int index) {
    int l_index = 2 * index + 1;
    int r_index = 2 * index + 2;
    if (l_index >= size()) { return; }
    if (r_index == size()) {
        if (*(data[index]) > *(data[l_index])) { swap_val(index, l_index); }
        return;
    }
    int less_index = *(data[l_index]) < *(data[r_index]) ? l_index : r_index;
    if (*(data[index]) > *(data[less_index])) {
        swap_val(index, less_index);
        bubble_down(less_index);
    } else {
        return;
    }
}

// swap the values at the two index: i1 and i2
template<typename T>
void Heap<T>::swap_val(int i1, int i2) {
    T *temp = data[i1];
    data[i1] = data[i2];
    data[i2] = temp;
}

template<typename T>
void Heap<T>::clear() {
    data.clear();
}

