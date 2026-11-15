# Wall Crack Detector - Java Application for Detecting Cracks in Walls

This is a simple and easy-to-use desktop application made in Java. It helps you find and mark cracks on wall photos automatically. You just have to load any wall photo, and this software will scan the photo, find crack-like lines, mark them with bright colours, and give you a detailed list with measurements like crack size and location.

It works completely on your computer without needing any internet connection, Python, or machine learning models. It uses the popular ImageJ library for image processing.

---

## Application Preview

Here are sample screenshots showing how the application looks and works:

| Desktop Workspace & Controls | Detected Crack Result |
| :---: | :---: |
| <img src="docs/images/sample%20UI.jpeg" alt="Sample UI Screen" width="450"/> | <img src="docs/images/demo.jpeg" alt="Crack Detection Demo" width="450"/> |
| *Desktop workspace with image viewer, settings, and table* | *Detected crack marked with magenta color and red boundary box* |

---

## Table of Contents

- [What Does This Project Do?](#what-does-this-project-do)
- [How Code is Organized](#how-code-is-organized)
- [Step-by-Step Flow of the Project](#step-by-step-flow-of-the-project)
- [How to Run the Project](#how-to-run-the-project)
- [Project Folder Structure](#project-folder-structure)

---

## What Does This Project Do?

In simple words, this project helps civil engineers, building supervisors, and students to quickly check wall photos for cracks without doing manual marking.

Here is what the project does in brief:

1. **Loads Photos Easily**: You can open any common wall photograph (PNG or JPEG) from your computer, or just drag and drop it into the app window.
2. **Cleans the Image**: Wall pictures often have uneven room light and shadows. The application automatically removes bad shadows so that only the actual surface is visible clearly.
3. **Catches Dark Lines (Cracks)**: It automatically scans dark lines running across the wall surface using automated thresholding.
4. **Removes Noise**: It is smart enough to ignore small circular dirt spots, nail holes, and dust dots, picking only long, thin crack lines.
5. **Highlights Cracks Clearly**: 
   - Marked crack pixels are shown in bright **magenta (pink-purple)** colour.
   - Each separate crack gets a numbered **red box** around it.
6. **Shows Complete Measurements Table**: It calculates and displays the exact size, pixel count, diagonal length, circularity, and thickness of every crack found.
7. **Interactive Click & Zoom**: If you click on any crack row in the table, the image immediately zooms and moves directly to that crack so you can inspect it closely.
8. **Export Reports**: With one click, it saves:
   - An overlay picture showing all markings (`overlay.png`)
   - A black-and-white crack mask (`mask.png`)
   - An Excel-friendly CSV table (`regions.csv`)
   - A JSON report for saving records (`report.json`)
