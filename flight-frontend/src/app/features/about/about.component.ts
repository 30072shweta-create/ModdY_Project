import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

interface TeamMember {
  name: string;
  role: string;
  image: string;
  bio: string;
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
      name: 'Capt. Rajesh Varma',
      role: 'Chief Executive Officer & Founder',
      image: 'https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=500&q=80',
      bio: 'Former commercial airline captain with 22+ years of aviation leadership across global airlines.'
    },
    {
      name: 'Elena Rostova',
      role: 'Chief Technology Officer',
      image: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=500&q=80',
      bio: 'Pioneered cloud-native reservation engines, real-time distributed seat locking, and AI flight dispatch systems.'
    },
    {
      name: 'Marcus Thorne',
      role: 'VP of Flight Operations & Safety',
      image: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=500&q=80',
      bio: 'Certified ICAO Flight Inspector with exemplary records in global route safety and fleet dispatch.'
    },
    {
      name: 'Ananya Sharma',
      role: 'Head of In-Flight Hospitality',
      image: 'https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&w=500&q=80',
      bio: 'Oversees 5-star passenger cabin comfort, culinary menus, and internationally recognized service training.'
    }
  ];
}
