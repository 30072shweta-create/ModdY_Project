import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

interface TeamMember {
  name: string;
  image: string;
  phrase: string;
}

interface FleetItem {
  model: string;
  type: string;
  capacity: string;
  range: string;
  image: string;
  features: string;
}

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './about.component.html',
  styleUrls: ['./about.component.scss']
})
export class AboutComponent {
  public fleet: FleetItem[] = [
    {
      model: 'Boeing 787-9 Dreamliner',
      type: 'Long-Haul International',
      capacity: '290 Passengers',
      range: '14,140 km',
      image: 'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?auto=format&fit=crop&w=800&q=80',
      features: 'Full lie-flat Business suites, larger dimmable windows, lower cabin altitude for reduced jet lag.'
    },
    {
      model: 'Airbus A350-900',
      type: 'Ultra Long-Haul Flagship',
      capacity: '325 Passengers',
      range: '15,000 km',
      image: 'https://images.unsplash.com/photo-1569154941061-e231b4725ef1?auto=format&fit=crop&w=800&q=80',
      features: 'High-efficiency carbon composite airframe, 4K in-flight entertainment at every seat, ambient LED mood lighting.'
    },
    {
      model: 'Airbus A321neo',
      type: 'Regional & Domestic Express',
      capacity: '192 Passengers',
      range: '7,400 km',
      image: 'https://images.unsplash.com/photo-1506015391300-4802dc74de2e?auto=format&fit=crop&w=800&q=80',
      features: '20% reduced fuel burn, whisper-quiet cabin acoustics, high-speed gate-to-gate satellite Wi-Fi.'
    }
  ];

  public leadership: TeamMember[] = [
    {
      name: 'Aparna Parashar',
      image: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=500&q=80',
      phrase: '“Crafting effortless, elevated journeys for every traveler who flies with Meridian.”'
    },
    {
      name: 'Shweta Rana',
      image: 'https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&w=500&q=80',
      phrase: '“Transforming modern air travel through seamless and intuitive digital innovation.”'
    },
    {
      name: 'Spoorthi G Talanki',
      image: 'https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?auto=format&fit=crop&w=500&q=80',
      phrase: '“Redefining commercial aviation with unwavering precision, safety, and operational excellence.”'
    },
    {
      name: 'Bhavana N K',
      image: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=500&q=80',
      phrase: '“Delivering genuine warmth, luxury, and peace of mind across every single mile.”'
    }
  ];
}
