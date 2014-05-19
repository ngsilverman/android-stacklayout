# Android StackLayout

Custom ViewGroup of which the top element can be swept away in all directions.

## Attributes

Name        | Format     | Description
------------|------------|-------------
infinite    | boolean    |  If true swept views will be added back to the bottom of the stack, otherwise they are removed entirely.

To use these attributes the following attribute must also be set on the root element of the XML layout:
`xmlns:custom="http://schemas.android.com/apk/res-auto"`

## Public Methods

Returns | Method                                   | Description
--------|------------------------------------------|-------------
void    | setAdapter(Adapter&nbsp;adapter)         | Sets the Adapter used to provide the data which backs this Stack.
void    | setListener(StaskListener&nbsp;listener) | Register callbacks to be invoked when swipes and hovers occur.

## Example

```xml
<red.rabbit.stack.StackLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:custom="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    custom:infinite="true" >

    <View
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#cc0000" />

    <View
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#00cc00" />

    <View
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#0000cc" />

</red.rabbit.stack.StackLayout>
```
