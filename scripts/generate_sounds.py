from pydub import AudioSegment
from pydub.generators import Sine, Square, WhiteNoise
import os

def generate_trade_success():
    # Create a positive "cha-ching" sound
    # First tone
    tone1 = Sine(800).to_audio_segment(duration=100)
    # Second tone
    tone2 = Sine(1200).to_audio_segment(duration=100)
    # Combine with a slight delay
    sound = tone1 + AudioSegment.silent(duration=50) + tone2
    # Add a slight fade
    sound = sound.fade_in(10).fade_out(50)
    return sound

def generate_trade_fail():
    # Create a negative "buzz" sound
    # Generate white noise
    noise = WhiteNoise().to_audio_segment(duration=200)
    # Add a low square wave for the buzz effect
    buzz = Square(200).to_audio_segment(duration=200)
    # Combine and adjust volume
    sound = noise.overlay(buzz)
    sound = sound - 10  # Reduce volume
    # Add fade
    sound = sound.fade_in(10).fade_out(100)
    return sound

def generate_market_update():
    # Create a subtle "tick" sound
    # Generate a short, high-pitched tone
    tick = Sine(2000).to_audio_segment(duration=50)
    # Add a slight echo effect
    echo = tick - 6  # Quieter echo
    sound = tick + AudioSegment.silent(duration=30) + echo
    # Add fade
    sound = sound.fade_in(5).fade_out(20)
    return sound

def main():
    # Create output directory if it doesn't exist
    output_dir = "../src/main/resources/assets/stockmarketmod/sounds"
    os.makedirs(output_dir, exist_ok=True)
    
    # Generate and save sounds
    trade_success = generate_trade_success()
    trade_success.export(os.path.join(output_dir, "trade_success.ogg"), format="ogg")
    
    trade_fail = generate_trade_fail()
    trade_fail.export(os.path.join(output_dir, "trade_fail.ogg"), format="ogg")
    
    market_update = generate_market_update()
    market_update.export(os.path.join(output_dir, "market_update.ogg"), format="ogg")
    
    print("Sound files generated successfully!")

if __name__ == "__main__":
    main() 