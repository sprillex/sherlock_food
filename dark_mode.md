# Robust Dark Mode Implementation

A guide and starter template for implementing a robust, accessible, and "eye-friendly" dark mode for web applications. 

This implementation goes beyond simple color inversion. It focuses on reducing eye strain, preventing OLED smearing, ensuring accessibility compliance, and managing visual hierarchy through elevation.

## 🎨 Best Practices & Design Principles

### 1. The Foundation: Dark Grey, Not Black
* **Avoid `#000000`:** Pure black causes "smearing" on OLED screens when pixels turn on/off and creates high-contrast "halation" (blurring) around text.
* **Use `#121212`:** This standard dark grey reduces eye strain and allows for shadows to be visible.

### 2. Typography & Contrast
* **Opacity over Color:** Instead of specific hex codes for text, use white with reduced opacity to blend naturally with different background elevations.
    * *High Emphasis:* 87% opacity
    * *Medium Emphasis:* 60% opacity
    * *Disabled:* 38% opacity
* **Avoid Pure White Text:** Pure white (`#FFFFFF`) on dark backgrounds can be visually vibrating.

### 3. Color Desaturation
* **Desaturate Accents:** Bright brand colors (like deep blue) often vibrate or become unreadable against dark backgrounds.
* **Solution:** Desaturate and lighten accent colors (e.g., turn Deep Blue `#0055ff` into Pastel Blue `#8ab4f8`) to meet WCAG contrast standards.

### 4. Depth via Elevation
* **Lightness = Closeness:** Since you cannot cast a shadow on a black void, use lightness to indicate depth. The "closer" an element is to the user (like a modal or card), the lighter the grey background should be.
    * *Background:* Level 0 (`#121212`)
    * *Card:* Level 1 (`#1e1e1e`)
    * *Modal:* Level 2 (`#2d2d2d`)

---

## 🚀 Starter Template

This template uses **CSS Custom Properties (Variables)** for theming and includes a blocking script to prevent the "Flash of Light" (FOUC) on page load.

### `index.html`

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Robust Dark Mode Template</title>
    <style>
        /* 1. Define the Color System */
        :root {
            /* LIGHT THEME DEFAULTS */
            --bg-body: #ffffff;
            --bg-surface-1: #f7f7f7; /* Cards */
            --bg-surface-2: #eeeeee; /* Modals/Dropdowns */
            
            --text-primary: #121212;
            --text-secondary: #5f6368;
            
            --brand-color: #0055ff;
            --error-color: #d32f2f;
            
            --border-color: #e0e0e0;
            --shadow-color: rgba(0, 0, 0, 0.1);
        }

        /* DARK THEME OVERRIDES */
        /* Applied when data-theme="dark" is on <html> OR system pref is dark */
        [data-theme="dark"] {
            /* Foundation: Dark Grey, not Black */
            --bg-body: #121212; 
            
            /* Elevation via Lightness (lighter = closer to user) */
            --bg-surface-1: #1e1e1e; 
            --bg-surface-2: #2d2d2d; 
            
            /* Typography: Off-white and Opacity */
            --text-primary: rgba(255, 255, 255, 0.87);
            --text-secondary: rgba(255, 255, 255, 0.60);
            
            /* Desaturated Accents */
            --brand-color: #8ab4f8; /* Lighter, pastel blue */
            --error-color: #f28b82; /* Lighter red/pink */
            
            --border-color: #333333;
            --shadow-color: rgba(0, 0, 0, 0.5);
        }

        /* 2. Global Styles */
        body {
            background-color: var(--bg-body);
            color: var(--text-primary);
            font-family: system-ui, -apple-system, sans-serif;
            margin: 0;
            padding: 2rem;
            line-height: 1.6;
            transition: background-color 0.3s ease, color 0.3s ease;
        }

        /* 3. Component Examples */
        .card {
            background-color: var(--bg-surface-1);
            padding: 2rem;
            border-radius: 8px;
            border: 1px solid var(--border-color);
            box-shadow: 0 4px 6px var(--shadow-color);
            max-width: 600px;
            margin-bottom: 2rem;
        }

        h1, h2 {
            margin-top: 0;
        }

        .secondary-text {
            color: var(--text-secondary);
            font-size: 0.9rem;
        }

        button {
            background-color: var(--brand-color);
            color: #121212; /* Keep text dark on brand buttons for contrast */
            border: none;
            padding: 10px 20px;
            border-radius: 4px;
            font-weight: 600;
            cursor: pointer;
        }

        /* Image handling for dark mode to reduce glare */
        [data-theme="dark"] img {
            filter: brightness(0.8) contrast(1.2);
        }
    </style>

    <script>
        // Check local storage or system preference immediately
        const savedTheme = localStorage.getItem('theme');
        const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

        if (savedTheme === 'dark' || (!savedTheme && systemPrefersDark)) {
            document.documentElement.setAttribute('data-theme', 'dark');
        }
    </script>
</head>
<body>

    <h1>Robust Dark Mode</h1>
    
    <div class="card">
        <h2>Surface Level 1</h2>
        <p>This card sits on top of the background. In dark mode, it is slightly lighter (#1e1e1e) than the body (#121212) to create elevation without relying solely on shadows.</p>
        <p class="secondary-text">This is secondary text. It uses opacity rather than a dark grey hex code, ensuring it blends correctly with any background color.</p>
        <br>
        <button id="theme-toggle">Toggle Theme</button>
    </div>

    <script>
        const toggleBtn = document.getElementById('theme-toggle');
        const html = document.documentElement;

        toggleBtn.addEventListener('click', () => {
            const currentTheme = html.getAttribute('data-theme');
            let newTheme = 'light';

            if (currentTheme !== 'dark') {
                newTheme = 'dark';
            }

            html.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
        });
    </script>
</body>
</html>
