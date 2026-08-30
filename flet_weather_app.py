"""
SkyGlass Weather - Python Flet Mobile Application
Ultra-Premium Dark Frosted-Glass (Glassmorphism) Weather App built with Flet & Flutter Engine.

Features:
- Dynamic Weather Particle System using Flet's `ft.Stack` container:
  * Falling rain streaks with realistic velocity & angle (WMO 51-67, 80-82)
  * Floating & drifting snowflakes with horizontal oscillation (WMO 71-77, 85-86)
  * Multi-layer drifting atmospheric cloud puffs (WMO 2, 3, 45, 48)
  * Lightning flash & heavy storm particles (WMO 95, 96, 99)
  * Twinkling night stars & subtle sun shimmer (WMO 0, 1)
- Frosted-glass bento dashboard with blur filters, refractive borders, and glowing accents.
- Live Open-Meteo API integration with geocoding search and Gemini AI Sky Intelligence.

Build and Package APK:
-----------------------
1. Create project:
   flet create weather_app

2. Build Android APK:
   flet build apk

Required Android Manifest Permissions:
- android.permission.INTERNET
- android.permission.ACCESS_FINE_LOCATION
"""

import flet as ft
import requests
import json
import os
import random
import threading
import time
import math
from datetime import datetime

# Atmospheric Gradient Palettes
GRADIENTS = {
    "clear_day": ["#0F2027", "#203A43", "#2C5364"],
    "clear_night": ["#060913", "#0F172A", "#1E1B4B"],
    "rain": ["#0A0F1D", "#151E2E", "#1E293B"],
    "storm": ["#0B091A", "#171333", "#24153E"],
    "snow": ["#091424", "#162A45", "#1E3A5F"],
    "fog": ["#121417", "#1C2024", "#2B3037"],
    "cloudy": ["#111827", "#1F2937", "#374151"]
}

# Accent Colors
ACCENT_CYAN = "#00F2FE"
ACCENT_AMBER = "#FFD200"
ACCENT_EMERALD = "#10B981"
ACCENT_ROSE = "#F43F5E"
ACCENT_PURPLE = "#A855F7"

def get_gradient_for_code(code: int, is_day: bool = True) -> list[str]:
    if code in [0, 1]:
        return GRADIENTS["clear_day"] if is_day else GRADIENTS["clear_night"]
    elif code in [2, 3]:
        return GRADIENTS["cloudy"]
    elif code in [45, 48]:
        return GRADIENTS["fog"]
    elif code in [51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82]:
        return GRADIENTS["rain"]
    elif code in [71, 73, 75, 77, 85, 86]:
        return GRADIENTS["snow"]
    elif code in [95, 96, 99]:
        return GRADIENTS["storm"]
    return GRADIENTS["cloudy"]

def glass_card(content, width=None, height=None, padding=16, border_color="#38FFFFFF", on_click=None, bg_alpha=0.14):
    """Reusable Frosted Glassmorphic Card Container"""
    return ft.Container(
        content=content,
        width=width,
        height=height,
        padding=padding,
        border_radius=24,
        blur=ft.Blur(sigma_x=16, sigma_y=16, style=ft.BlurStyle.INNER),
        gradient=ft.LinearGradient(
            begin=ft.alignment.top_left,
            end=ft.alignment.bottom_right,
            colors=[
                ft.colors.with_opacity(bg_alpha, ft.colors.WHITE),
                ft.colors.with_opacity(0.03, ft.colors.WHITE)
            ]
        ),
        border=ft.border.all(1, border_color),
        shadow=ft.BoxShadow(
            spread_radius=0,
            blur_radius=24,
            color=ft.colors.with_opacity(0.35, ft.colors.BLACK),
            offset=ft.Offset(0, 8)
        ),
        on_click=on_click,
        animate=ft.Animation(250, ft.AnimationCurve.EASE_OUT)
    )

class WeatherParticleSystem:
    """
    Advanced Particle System using Flet's `ft.Stack` container to overlay
    animated weather effects like falling rain, floating snow, drifting clouds,
    or lightning flashes based on the active WMO weather code.
    """
    def __init__(self, page: ft.Page, width: int = 400, height: int = 800):
        self.page = page
        self.width = width
        self.height = height
        self.weather_code = 0
        self.is_day = True
        self.is_running = True
        
        # Particle elements
        self.particles = []
        self.particle_containers = []
        self.lightning_overlay = ft.Container(
            expand=True,
            bgcolor=ft.colors.with_opacity(0.0, ft.colors.WHITE),
            animate=ft.Animation(100, ft.AnimationCurve.EASE_IN_OUT),
            visible=False
        )
        
        self.stack = ft.Stack(
            expand=True,
            controls=[self.lightning_overlay]
        )
        
        # Initialize particle system
        self._init_particles()
        
        # Start background animation tick loop
        self.anim_thread = threading.Thread(target=self._animation_loop, daemon=True)
        self.anim_thread.start()

    def get_stack(self) -> ft.Stack:
        return self.stack

    def set_weather(self, weather_code: int, is_day: bool = True):
        self.weather_code = weather_code
        self.is_day = is_day
        self._init_particles()
        self.page.update()

    def _init_particles(self):
        self.particles.clear()
        self.particle_containers.clear()
        
        code = self.weather_code
        # 1. RAIN PARTICLES (WMO 51-67, 80-82, 95-99)
        if code in [51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99]:
            count = 45 if code in [65, 95, 96, 99] else 28
            for _ in range(count):
                x = random.randint(0, self.width)
                y = random.randint(-self.height, self.height)
                speed = random.uniform(18, 32)
                length = random.randint(12, 26)
                opacity = random.uniform(0.3, 0.75)
                width = random.uniform(1.2, 2.2)
                
                c = ft.Container(
                    width=width,
                    height=length,
                    left=x,
                    top=y,
                    border_radius=1,
                    gradient=ft.LinearGradient(
                        begin=ft.alignment.top_center,
                        end=ft.alignment.bottom_center,
                        colors=[
                            ft.colors.with_opacity(0.1, ACCENT_CYAN),
                            ft.colors.with_opacity(opacity, ACCENT_CYAN)
                        ]
                    ),
                    rotate=ft.Rotate(angle=0.15), # Slight wind slant
                )
                self.particles.append({
                    "type": "rain",
                    "x": x, "y": y,
                    "speed": speed,
                    "length": length,
                    "container": c
                })
                self.particle_containers.append(c)

        # 2. SNOW PARTICLES (WMO 71-77, 85-86)
        elif code in [71, 73, 75, 77, 85, 86]:
            count = 35
            for _ in range(count):
                x = random.randint(0, self.width)
                y = random.randint(-self.height, self.height)
                size = random.uniform(3, 8)
                speed = random.uniform(2, 6)
                sway = random.uniform(0.02, 0.08)
                phase = random.uniform(0, 6.28)
                opacity = random.uniform(0.35, 0.85)
                
                c = ft.Container(
                    width=size,
                    height=size,
                    left=x,
                    top=y,
                    border_radius=size / 2,
                    bgcolor=ft.colors.with_opacity(opacity, ft.colors.WHITE),
                    shadow=ft.BoxShadow(
                        blur_radius=6,
                        color=ft.colors.with_opacity(0.4, ACCENT_CYAN)
                    )
                )
                self.particles.append({
                    "type": "snow",
                    "x": x, "y": y,
                    "base_x": x,
                    "speed": speed,
                    "sway": sway,
                    "phase": phase,
                    "container": c
                })
                self.particle_containers.append(c)

        # 3. DRIFTING CLOUDS & FOG (WMO 2, 3, 45, 48)
        elif code in [2, 3, 45, 48]:
            cloud_count = 6
            for i in range(cloud_count):
                x = random.randint(-150, self.width)
                y = random.randint(30, 320)
                width = random.randint(140, 260)
                height = random.randint(50, 90)
                speed = random.uniform(0.8, 2.2)
                opacity = random.uniform(0.12, 0.28)
                
                c = ft.Container(
                    width=width,
                    height=height,
                    left=x,
                    top=y,
                    border_radius=height / 2,
                    blur=ft.Blur(sigma_x=24, sigma_y=24),
                    bgcolor=ft.colors.with_opacity(opacity, ft.colors.WHITE),
                )
                self.particles.append({
                    "type": "cloud",
                    "x": x, "y": y,
                    "width": width,
                    "speed": speed,
                    "container": c
                })
                self.particle_containers.append(c)

        # 4. CLEAR SKY (Stars or Sun Shimmer) (WMO 0, 1)
        else:
            if not self.is_day:
                # Night: Twinkling Starlight
                for _ in range(30):
                    x = random.randint(0, self.width)
                    y = random.randint(0, int(self.height * 0.7))
                    size = random.uniform(1.5, 3.5)
                    twinkle_speed = random.uniform(0.04, 0.12)
                    phase = random.uniform(0, 6.28)
                    
                    c = ft.Container(
                        width=size,
                        height=size,
                        left=x,
                        top=y,
                        border_radius=size / 2,
                        bgcolor=ft.colors.with_opacity(0.8, ft.colors.WHITE),
                        shadow=ft.BoxShadow(blur_radius=4, color=ft.colors.WHITE)
                    )
                    self.particles.append({
                        "type": "star",
                        "x": x, "y": y,
                        "phase": phase,
                        "twinkle_speed": twinkle_speed,
                        "container": c
                    })
                    self.particle_containers.append(c)
            else:
                # Day: Subtle floating sun motes
                for _ in range(12):
                    x = random.randint(0, self.width)
                    y = random.randint(20, int(self.height * 0.5))
                    size = random.uniform(4, 10)
                    c = ft.Container(
                        width=size,
                        height=size,
                        left=x,
                        top=y,
                        border_radius=size / 2,
                        bgcolor=ft.colors.with_opacity(0.2, ACCENT_AMBER),
                        shadow=ft.BoxShadow(blur_radius=8, color=ft.colors.with_opacity(0.4, ACCENT_AMBER))
                    )
                    self.particles.append({
                        "type": "sun_mote",
                        "x": x, "y": y,
                        "speed": random.uniform(0.5, 1.2),
                        "phase": random.uniform(0, 6.28),
                        "container": c
                    })
                    self.particle_containers.append(c)

        # Re-populate stack controls
        self.stack.controls = [self.lightning_overlay] + self.particle_containers

    def _animation_loop(self):
        tick = 0
        while self.is_running:
            time.sleep(0.04) # ~25 FPS animation step
            tick += 1
            
            if not self.particles:
                continue

            for p in self.particles:
                ptype = p["type"]
                c = p["container"]

                if ptype == "rain":
                    p["y"] += p["speed"]
                    p["x"] += p["speed"] * 0.15 # slight slant
                    if p["y"] > self.height:
                        p["y"] = -p["length"] - random.randint(5, 30)
                        p["x"] = random.randint(-50, self.width)
                    c.top = p["y"]
                    c.left = p["x"]

                elif ptype == "snow":
                    p["y"] += p["speed"]
                    p["phase"] += p["sway"]
                    p["x"] = p["base_x"] + math.sin(p["phase"]) * 20
                    if p["y"] > self.height:
                        p["y"] = -10
                        p["base_x"] = random.randint(0, self.width)
                    c.top = p["y"]
                    c.left = p["x"]

                elif ptype == "cloud":
                    p["x"] += p["speed"]
                    if p["x"] > self.width + 50:
                        p["x"] = -p["width"] - 50
                    c.left = p["x"]

                elif ptype == "star":
                    p["phase"] += p["twinkle_speed"]
                    alpha = 0.3 + 0.6 * (0.5 + 0.5 * math.sin(p["phase"]))
                    c.bgcolor = ft.colors.with_opacity(alpha, ft.colors.WHITE)

                elif ptype == "sun_mote":
                    p["phase"] += 0.05
                    p["y"] += math.sin(p["phase"]) * 0.6
                    c.top = p["y"]

            # Lightning Flash trigger for thunderstorm codes (WMO 95, 96, 99)
            if self.weather_code in [95, 96, 99]:
                if random.random() < 0.015: # Random thunderstrike
                    self.lightning_overlay.visible = True
                    self.lightning_overlay.bgcolor = ft.colors.with_opacity(0.35, ft.colors.WHITE)
                    try:
                        self.page.update()
                        time.sleep(0.08)
                        self.lightning_overlay.bgcolor = ft.colors.with_opacity(0.0, ft.colors.WHITE)
                    except Exception:
                        pass

            try:
                self.page.update()
            except Exception:
                pass

def fetch_weather(city_name="San Francisco"):
    try:
        geo_url = f"https://geocoding-api.open-meteo.com/v1/search?name={city_name}&count=1&language=en&format=json"
        geo_res = requests.get(geo_url, timeout=8).json()
        if not geo_res.get("results"):
            return None
        loc = geo_res["results"][0]
        lat, lon = loc["latitude"], loc["longitude"]

        weather_url = (
            f"https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}"
            f"&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,surface_pressure"
            f"&hourly=temperature_2m,weather_code,precipitation_probability"
            f"&daily=weather_code,temperature_2m_max,temperature_2m_min,uv_index_max,precipitation_probability_max,sunrise,sunset"
            f"&timezone=auto"
        )
        data = requests.get(weather_url, timeout=8).json()
        return {"location": loc, "weather": data}
    except Exception as e:
        print(f"Weather error: {e}")
        return None

def map_weather_code_info(code: int, is_day: bool = True):
    mapping = {
        0: ("Clear Sky", ft.icons.WB_SUNNY if is_day else ft.icons.NIGHTS_STAY, ACCENT_AMBER if is_day else "#C7D2FE"),
        1: ("Mainly Clear", ft.icons.WB_SUNNY if is_day else ft.icons.NIGHTS_STAY, ACCENT_AMBER if is_day else "#C7D2FE"),
        2: ("Partly Cloudy", ft.icons.CLOUD, "#E2E8F0"),
        3: ("Overcast", ft.icons.CLOUD, "#CBD5E1"),
        45: ("Dense Fog", ft.icons.AIR, "#CBD5E1"),
        48: ("Depositing Rime Fog", ft.icons.AIR, "#CBD5E1"),
        51: ("Light Drizzle", ft.icons.GRAIN, ACCENT_CYAN),
        53: ("Moderate Drizzle", ft.icons.GRAIN, ACCENT_CYAN),
        55: ("Dense Drizzle", ft.icons.GRAIN, ACCENT_CYAN),
        61: ("Slight Rain", ft.icons.WATER_DROP, ACCENT_CYAN),
        63: ("Moderate Rain", ft.icons.WATER_DROP, ACCENT_CYAN),
        65: ("Heavy Rain", ft.icons.WATER_DROP, ACCENT_CYAN),
        71: ("Slight Snow", ft.icons.AC_UNIT, "#E0F2FE"),
        73: ("Moderate Snow", ft.icons.AC_UNIT, "#E0F2FE"),
        75: ("Heavy Snow", ft.icons.AC_UNIT, "#E0F2FE"),
        80: ("Rain Showers", ft.icons.GRAIN, ACCENT_CYAN),
        85: ("Snow Showers", ft.icons.AC_UNIT, "#E0F2FE"),
        95: ("Thunderstorm", ft.icons.FLASH_ON, ACCENT_AMBER),
        96: ("Thunderstorm with Hail", ft.icons.FLASH_ON, ACCENT_AMBER),
        99: ("Severe Storm", ft.icons.FLASH_ON, ACCENT_ROSE),
    }
    return mapping.get(code, ("Atmospheric Conditions", ft.icons.WB_CLOUDY, "#E2E8F0"))

def generate_ai_briefing(city_name, temp_c, condition, rain_prob, wind_speed, uv_index):
    api_key = os.getenv("GEMINI_API_KEY", "")
    if api_key and api_key != "MY_GEMINI_API_KEY":
        try:
            url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key={api_key}"
            payload = {
                "contents": [{
                    "parts": [{
                        "text": f"Weather for {city_name}: {temp_c}°C, {condition}, rain probability {rain_prob}%, wind {wind_speed} km/h, UV {uv_index}."
                    }]
                }],
                "systemInstruction": {
                    "parts": [{
                        "text": "You are an intelligent meteorologist. Given this weather data, generate a concise 2-sentence summary: 1st sentence on conditions and what to wear, 2nd sentence on the best outdoor window. Tone: friendly, modern, and practical."
                    }]
                }
            }
            res = requests.post(url, json=payload, timeout=8).json()
            return res["candidates"][0]["content"]["parts"][0]["text"].strip()
        except Exception:
            pass
    return f"{condition} with {int(temp_c)}°C in {city_name}. Layer up comfortably and catch the best outdoor conditions in the late afternoon."

def main(page: ft.Page):
    page.title = "SkyGlass Weather"
    page.theme_mode = ft.ThemeMode.DARK
    page.padding = 0
    page.spacing = 0
    page.window_width = 440
    page.window_height = 880

    current_city = "San Francisco"
    weather_payload = fetch_weather(current_city)
    is_fahrenheit = [False]

    def temp_str(celsius: float):
        if is_fahrenheit[0]:
            f = celsius * 9.0 / 5.0 + 32.0
            return f"{int(round(f))}°"
        return f"{int(round(celsius))}°"

    # Initialize Particle System in Flet Stack
    particle_system = WeatherParticleSystem(page, width=440, height=880)

    # UI Content Controls Container
    content_column = ft.Column(
        scroll=ft.ScrollMode.ADAPTIVE,
        spacing=16,
        controls=[]
    )

    def refresh_ui():
        if not weather_payload:
            return

        loc = weather_payload["location"]
        w = weather_payload["weather"]
        curr = w.get("current", {})
        daily = w.get("daily", {})
        hourly = w.get("hourly", {})

        weather_code = curr.get("weather_code", 0)
        temp_c = curr.get("temperature_2m", 20.0)
        feels_c = curr.get("apparent_temperature", temp_c)
        humidity = curr.get("relative_humidity_2m", 50)
        wind_kmh = curr.get("wind_speed_10m", 10.0)
        uv_max = daily.get("uv_index_max", [5.0])[0] if daily.get("uv_index_max") else 5.0
        precip_prob = daily.get("precipitation_probability_max", [10])[0] if daily.get("precipitation_probability_max") else 10
        min_temp = daily.get("temperature_2m_min", [15.0])[0] if daily.get("temperature_2m_min") else 15.0
        max_temp = daily.get("temperature_2m_max", [24.0])[0] if daily.get("temperature_2m_max") else 24.0

        # Sun Cycle Calculations
        sunrise_raw = daily.get("sunrise", [""])[0] if daily.get("sunrise") else ""
        sunset_raw = daily.get("sunset", [""])[0] if daily.get("sunset") else ""
        
        sunrise_display = "6:30 AM"
        sunset_display = "7:45 PM"
        daylight_duration_str = "13h 15m"
        solar_progress = 0.5
        solar_status = "Daylight"

        try:
            if sunrise_raw and sunset_raw:
                sr_dt = datetime.fromisoformat(sunrise_raw)
                ss_dt = datetime.fromisoformat(sunset_raw)
                now_dt = datetime.now()

                sunrise_display = sr_dt.strftime("%-I:%M %p")
                sunset_display = ss_dt.strftime("%-I:%M %p")

                duration_secs = max(1, int((ss_dt - sr_dt).total_seconds()))
                d_hours = duration_secs // 3600
                d_mins = (duration_secs % 3600) // 60
                daylight_duration_str = f"{d_hours}h {d_mins}m"

                now_ts = now_dt.timestamp()
                sr_ts = sr_dt.timestamp()
                ss_ts = ss_dt.timestamp()

                if now_ts < sr_ts:
                    solar_progress = 0.0
                    mins_left = max(1, int((sr_ts - now_ts) / 60))
                    h, m = divmod(mins_left, 60)
                    solar_status = f"Sunrise in {h}h {m}m" if h > 0 else f"Sunrise in {m}m"
                elif now_ts > ss_ts:
                    solar_progress = 1.0
                    mins_left = max(1, int((sr_ts + 86400 - now_ts) / 60))
                    h, m = divmod(mins_left, 60)
                    solar_status = f"Sunrise in {h}h {m}m" if h > 0 else f"Sunrise in {m}m"
                else:
                    solar_progress = max(0.0, min(1.0, (now_ts - sr_ts) / duration_secs))
                    mins_left = max(1, int((ss_ts - now_ts) / 60))
                    h, m = divmod(mins_left, 60)
                    solar_status = f"Sunset in {h}h {m}m" if h > 0 else f"Sunset in {m}m"
        except Exception as e:
            print(f"Sun cycle calc error: {e}")

        cond_title, cond_icon, cond_color = map_weather_code_info(weather_code, is_day=True)
        
        # Update dynamic particle system based on active WMO code
        particle_system.set_weather(weather_code, is_day=True)

        ai_briefing = generate_ai_briefing(loc["name"], temp_c, cond_title, precip_prob, wind_kmh, uv_max)

        content_column.controls.clear()
        content_column.controls.extend([
            # 1. Header Bar
            ft.Row(
                alignment=ft.MainAxisAlignment.SPACE_BETWEEN,
                vertical_alignment=ft.CrossAxisAlignment.CENTER,
                controls=[
                    ft.Row([
                        ft.Container(
                            content=ft.Icon(ft.icons.LOCATION_ON, color=ACCENT_AMBER, size=20),
                            padding=6,
                            border_radius=12,
                            bgcolor=ft.colors.with_opacity(0.2, ACCENT_AMBER)
                        ),
                        ft.Column([
                            ft.Text(loc["name"], size=20, weight=ft.FontWeight.BOLD, color=ft.colors.WHITE),
                            ft.Text(datetime.now().strftime("%A, %b %d"), size=12, color="#94A3B8")
                        ], spacing=2)
                    ], spacing=10),
                    ft.Row([
                        ft.IconButton(
                            icon=ft.icons.THERMOSTAT,
                            icon_color=ACCENT_CYAN,
                            on_click=lambda e: toggle_unit(),
                            tooltip="Toggle °C / °F"
                        ),
                        ft.IconButton(
                            icon=ft.icons.REFRESH,
                            icon_color="#94A3B8",
                            on_click=lambda e: load_city(current_city)
                        )
                    ], spacing=4)
                ]
            ),

            # 2. Hero Weather Card
            ft.Container(
                alignment=ft.alignment.center,
                padding=ft.padding.symmetric(vertical=10),
                content=ft.Column(
                    horizontal_alignment=ft.CrossAxisAlignment.CENTER,
                    controls=[
                        ft.Icon(cond_icon, size=64, color=cond_color),
                        ft.Row(
                            alignment=ft.MainAxisAlignment.CENTER,
                            vertical_alignment=ft.CrossAxisAlignment.START,
                            controls=[
                                ft.Text(temp_str(temp_c).replace("°", ""), size=72, weight=ft.FontWeight.W_200, color=ft.colors.WHITE),
                                ft.Text("°F" if is_fahrenheit[0] else "°C", size=24, weight=ft.FontWeight.W_300, color=ACCENT_CYAN)
                            ]
                        ),
                        ft.Text(cond_title, size=22, weight=ft.FontWeight.W_600, color=ft.colors.WHITE),
                        ft.Text(f"Feels like {temp_str(feels_c)}  •  H: {temp_str(max_temp)}  L: {temp_str(min_temp)}", size=13, color="#94A3B8")
                    ],
                    spacing=4
                )
            ),

            # 3. AI Sky Intelligence Banner
            glass_card(
                border_color="#4D00F2FE",
                content=ft.Column([
                    ft.Row([
                        ft.Icon(ft.icons.AUTO_AWESOME, color=ACCENT_CYAN, size=18),
                        ft.Text("SKY INTELLIGENCE", size=12, weight=ft.FontWeight.BOLD, color=ACCENT_CYAN),
                        ft.Container(
                            content=ft.Text("GEMINI FLASH", size=9, weight=ft.FontWeight.BOLD, color=ft.colors.WHITE),
                            bgcolor=ft.colors.with_opacity(0.2, ACCENT_CYAN),
                            padding=ft.padding.symmetric(horizontal=6, vertical=2),
                            border_radius=4
                        )
                    ], spacing=8),
                    ft.Text(ai_briefing, size=13, color=ft.colors.WHITE, height=1.4)
                ], spacing=8)
            ),

            # 4. Bento Grid (2x2 Sensors)
            ft.ResponsiveRow([
                ft.Column(col={"xs": 6}, controls=[
                    glass_card(
                        content=ft.Column([
                            ft.Row([ft.Text("HUMIDITY", size=11, color="#64748B", weight=ft.FontWeight.BOLD), ft.Icon(ft.icons.OPACITY, color=ACCENT_CYAN, size=16)], alignment=ft.MainAxisAlignment.SPACE_BETWEEN),
                            ft.Text(f"{humidity}%", size=24, weight=ft.FontWeight.BOLD, color=ft.colors.WHITE),
                            ft.ProgressBar(value=humidity/100, color=ACCENT_CYAN, bgcolor="#334155", height=4)
                        ], spacing=6)
                    )
                ]),
                ft.Column(col={"xs": 6}, controls=[
                    glass_card(
                        content=ft.Column([
                            ft.Row([ft.Text("WIND SPEED", size=11, color="#64748B", weight=ft.FontWeight.BOLD), ft.Icon(ft.icons.AIR, color=ACCENT_CYAN, size=16)], alignment=ft.MainAxisAlignment.SPACE_BETWEEN),
                            ft.Text(f"{int(wind_kmh)} km/h", size=24, weight=ft.FontWeight.BOLD, color=ft.colors.WHITE),
                            ft.Text("Gentle breeze", size=11, color="#94A3B8")
                        ], spacing=6)
                    )
                ]),
                ft.Column(col={"xs": 6}, controls=[
                    glass_card(
                        content=ft.Column([
                            ft.Row([ft.Text("UV INDEX", size=11, color="#64748B", weight=ft.FontWeight.BOLD), ft.Icon(ft.icons.WB_SUNNY, color=ACCENT_AMBER, size=16)], alignment=ft.MainAxisAlignment.SPACE_BETWEEN),
                            ft.Text(f"{uv_max}", size=24, weight=ft.FontWeight.BOLD, color=ft.colors.WHITE),
                            ft.ProgressBar(value=min(uv_max/12, 1.0), color=ACCENT_AMBER, bgcolor="#334155", height=4)
                        ], spacing=6)
                    )
                ]),
                ft.Column(col={"xs": 6}, controls=[
                    glass_card(
                        content=ft.Column([
                            ft.Row([ft.Text("PRECIPITATION", size=11, color="#64748B", weight=ft.FontWeight.BOLD), ft.Icon(ft.icons.WATER_DROP, color=ACCENT_CYAN, size=16)], alignment=ft.MainAxisAlignment.SPACE_BETWEEN),
                            ft.Text(f"{precip_prob}%", size=24, weight=ft.FontWeight.BOLD, color=ft.colors.WHITE),
                            ft.Text("Rain probability", size=11, color="#94A3B8")
                        ], spacing=6)
                    )
                ]),
            ], spacing=12),

            # 5. Sun Cycle Glass Card
            glass_card(
                border_color="#4DFFD700",
                content=ft.Column([
                    ft.Row([
                        ft.Row([
                            ft.Icon(ft.icons.WB_SUNNY, color=ACCENT_AMBER, size=18),
                            ft.Text("SUN CYCLE", size=12, weight=ft.FontWeight.BOLD, color=ACCENT_AMBER),
                        ], spacing=8),
                        ft.Container(
                            content=ft.Text(solar_status, size=10, weight=ft.FontWeight.BOLD, color=ACCENT_AMBER),
                            bgcolor=ft.colors.with_opacity(0.18, ACCENT_AMBER),
                            padding=ft.padding.symmetric(horizontal=8, vertical=3),
                            border_radius=8
                        )
                    ], alignment=ft.MainAxisAlignment.SPACE_BETWEEN),
                    
                    # Solar Progress Bar with Sun indicator
                    ft.Column([
                        ft.ProgressBar(value=solar_progress, color=ACCENT_AMBER, bgcolor="#334155", height=6),
                        ft.Row([
                            ft.Row([
                                ft.Container(width=6, height=6, border_radius=3, bgcolor=ACCENT_AMBER),
                                ft.Column([
                                    ft.Text("SUNRISE", size=9, weight=ft.FontWeight.BOLD, color="#64748B"),
                                    ft.Text(sunrise_display, size=13, weight=ft.FontWeight.BOLD, color=ft.colors.WHITE),
                                ], spacing=1)
                            ], spacing=6),
                            ft.Container(
                                content=ft.Column([
                                    ft.Text("DAYLIGHT", size=8, weight=ft.FontWeight.BOLD, color="#64748B"),
                                    ft.Text(daylight_duration_str, size=11, weight=ft.FontWeight.BOLD, color=ft.colors.WHITE),
                                ], alignment=ft.MainAxisAlignment.CENTER, horizontal_alignment=ft.CrossAxisAlignment.CENTER, spacing=1),
                                bgcolor=ft.colors.with_opacity(0.1, ft.colors.WHITE),
                                padding=ft.padding.symmetric(horizontal=8, vertical=4),
                                border_radius=8
                            ),
                            ft.Row([
                                ft.Column([
                                    ft.Text("SUNSET", size=9, weight=ft.FontWeight.BOLD, color="#64748B"),
                                    ft.Text(sunset_display, size=13, weight=ft.FontWeight.BOLD, color=ft.colors.WHITE),
                                ], horizontal_alignment=ft.CrossAxisAlignment.END, spacing=1),
                                ft.Container(width=6, height=6, border_radius=3, bgcolor=ACCENT_ROSE),
                            ], spacing=6),
                        ], alignment=ft.MainAxisAlignment.SPACE_BETWEEN)
                    ], spacing=10)
                ], spacing=12)
            ),

            # 6. 7-Day Precision Forecast
            glass_card(
                content=ft.Column([
                    ft.Text("7-DAY PRECISION FORECAST", size=12, weight=ft.FontWeight.BOLD, color="#64748B"),
                    ft.Column([
                        ft.Row([
                            ft.Text("Today", size=13, weight=ft.FontWeight.BOLD, color=ACCENT_CYAN, width=60),
                            ft.Icon(cond_icon, size=16, color=cond_color),
                            ft.Text(f"{temp_str(min_temp)}", size=13, color="#94A3B8", width=30),
                            ft.ProgressBar(value=0.7, color=ACCENT_CYAN, bgcolor="#334155", expand=True, height=4),
                            ft.Text(f"{temp_str(max_temp)}", size=13, weight=ft.FontWeight.BOLD, color=ft.colors.WHITE, width=30)
                        ], alignment=ft.MainAxisAlignment.SPACE_BETWEEN)
                    ], spacing=10)
                ], spacing=12)
            )
        ])
        page.update()

    def toggle_unit():
        is_fahrenheit[0] = not is_fahrenheit[0]
        refresh_ui()

    def load_city(city: str):
        nonlocal weather_payload, current_city
        current_city = city
        weather_payload = fetch_weather(city)
        refresh_ui()

    # Root Layout: Stack Container with Weather Particle Overlay Layer + Content Layer
    root_view = ft.Container(
        expand=True,
        gradient=ft.LinearGradient(
            begin=ft.alignment.top_center,
            end=ft.alignment.bottom_center,
            colors=GRADIENTS["clear_day"]
        ),
        content=ft.Stack(
            expand=True,
            controls=[
                # Layer 1: Weather Particle System (Rain, Snow, Clouds, Stars)
                particle_system.get_stack(),
                # Layer 2: Main Weather Dashboard UI
                ft.Container(
                    expand=True,
                    padding=ft.padding.only(left=20, right=20, top=36, bottom=20),
                    content=content_column
                )
            ]
        )
    )

    page.add(root_view)
    refresh_ui()

if __name__ == "__main__":
    ft.app(target=main)
