# 🅿️ ParkEase: Parking Management System

Welcome to **ParkEase**! This is a simple yet powerful application designed to manage a parking lot. Whether you're a car owner or a parking lot manager, this system helps track which vehicles are parked, where they are, and how much they need to pay.

This guide is written for **total beginners**. We will explain how the code works behind the scenes using real-world analogies.

---

## 🚀 How to Run the Project
If you are using a code editor like VS Code or IntelliJ:
1. Open the project folder.
2. Find the file named `ParkingLotSystem.java`.
3. Click the **Run** button.

The app will open a "Dark Mode" window where you can start parking cars!

---

## 🧠 How it Works (The "Technical Stuff")

To build this app, we used **Java**, a popular programming language. We followed a concept called **Object-Oriented Programming (OOP)**. Imagine you are building a Lego set; every piece has a specific purpose.

### 1. The "Blueprints" (Classes)
In Java, we create "Classes" which are like blueprints. 

*   **Vehicle:** This is a general blueprint for anything with wheels. It knows the plate number and the owner's name.
*   **Car & Motorcycle:** These "inherit" from the Vehicle blueprint. Just like a real car is a type of vehicle, these classes take the basic info from `Vehicle` and add their own rules (like how much to charge for parking).
*   **ParkingSlot:** Think of this as a single "box" or space in the lot. It can either be empty or have a vehicle inside it.
*   **ParkingLot:** This is the big container that holds all the slots (20 in total for this project).

### 2. The Brain (ParkingManager)
The `ParkingManager` is like the supervisor of the parking lot. 
*   When a car arrives, the manager finds an empty slot.
*   The manager writes down a **ParkingRecord** (a receipt) that tracks when the car entered.
*   When the car leaves, the manager calculates the fee based on how long the car stayed.

### 3. Saving Your Data (Persistence)
Ever wondered what happens if you close the app? 
The system creates a file called `parking_records.txt`. Every time a car parks or leaves, the manager writes it down in this text file. When you open the app again, it reads this file so it remembers exactly which cars are still parked!

### 4. The Pretty Face (The GUI)
The `ParkingGUI` class creates the window you see on your screen. 
*   **Buttons:** Let you "Check In" or "Check Out".
*   **Live Map:** Shows a grid of 20 spaces. Green cars are parked; empty spaces look dark.
*   **Stats:** Shows you instantly how many spots are left.

---

## 🛠 Features for Beginners to Explore
*   **Check In:** Add a vehicle by typing its plate and owner name.
*   **Check Out:** Pick a parked car, and the app will tell you the price (PKR 500 for the first hour + PKR 10 per minute after).
*   **Active Bookings:** A list of everyone currently in the lot.
*   **History:** A log of every vehicle that has ever visited.

---

## 💡 Concepts Used (For Students)
If you are learning to code, look for these keywords in `ParkingLotSystem.java`:
*   **Inheritance:** `class Car extends Vehicle` (A Car *is a* Vehicle).
*   **Encapsulation:** Using `private` variables so data stays safe inside its "box".
*   **Polymorphism:** The `calculateFee` method works differently for Cars and Motorcycles, even though they are both Vehicles.
*   **File I/O:** Reading and writing to `parking_records.txt`.

---

## 📝 Note on the Files
*   `.java` files: These are the source code files that you can read and edit.
*   `.class` files: These are the "compiled" versions that the computer understands. You don't need to touch these!
*   `parking_records.txt`: This is where your data is stored.

Happy Coding! 🚗💨
