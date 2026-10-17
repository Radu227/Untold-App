import React, { useState } from 'react';
import { ChevronDown, ChevronUp, Info as InfoIcon, MapPin, Tent, Music } from 'lucide-react';

interface FAQItemProps {
  question: string;
  answer: string;
  isOpen: boolean;
  onClick: () => void;
}

const FAQItem: React.FC<FAQItemProps> = ({ question, answer, isOpen, onClick }) => {
  return (
    <div className="glass-panel rounded-xl mb-4 overflow-hidden transition-all duration-300">
      <button
        className="w-full px-6 py-4 flex items-center justify-between text-left focus:outline-none hover:bg-white/5 transition-colors"
        onClick={onClick}
      >
        <span className="font-semibold text-lg">{question}</span>
        {isOpen ? <ChevronUp className="text-[#00dbe9] w-5 h-5 flex-shrink-0" /> : <ChevronDown className="text-gray-400 w-5 h-5 flex-shrink-0" />}
      </button>
      <div
        className={`px-6 overflow-hidden transition-all duration-300 ease-in-out ${
          isOpen ? 'max-h-48 pb-4 opacity-100' : 'max-h-0 opacity-0'
        }`}
      >
        <p className="text-gray-300">{answer}</p>
      </div>
    </div>
  );
};

export const Info: React.FC = () => {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  const faqs = [
    {
      question: "What items are prohibited at the festival?",
      answer: "Weapons, illegal substances, professional cameras without a press pass, outside food and beverages, and large backpacks are strictly prohibited. Small bags and fanny packs are allowed and will be searched at the entrance."
    },
    {
      question: "Where can I find the first aid tents?",
      answer: "First aid tents are located near the Main Stage, the Food Court, and near the main entrance. Look for the white tents with a red cross. Check the Venue Map for exact locations."
    },
    {
      question: "Is there a lost and found?",
      answer: "Yes, the Lost & Found is located at the Info Point near the main entrance. Any items found should be dropped off there. You can also report lost items via the app."
    },
    {
      question: "Can I leave and re-enter the festival?",
      answer: "General admission tickets do not allow re-entry. VIP ticket holders may leave and re-enter once per day. Please plan accordingly."
    },
    {
      question: "How do I top up my festival wristband?",
      answer: "You can top up your wristband directly in this app under the 'My Account' section, or at any of the physical top-up stations located throughout the festival grounds."
    }
  ];

  return (
    <div className="container mx-auto px-4 py-24 flex-grow flex flex-col pt-32">
      <div className="flex items-center justify-center mb-8 gap-3">
        <InfoIcon className="w-10 h-10 text-[#00dbe9]" />
        <h1 className="text-4xl md:text-5xl font-bold gradient-text pb-2">Festival Info & FAQ</h1>
      </div>

      <div className="max-w-3xl mx-auto w-full">
        <p className="text-center text-gray-300 mb-10 text-lg">
          Everything you need to know to have the best experience at the festival. 
          If you can't find your answer here, visit our Info Points on site.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
          <div className="glass-panel p-6 rounded-xl flex flex-col items-center text-center glow-effect transition-all duration-300 hover:-translate-y-1">
            <MapPin className="w-8 h-8 text-[#00dbe9] mb-3" />
            <h3 className="font-semibold mb-2">Location</h3>
            <p className="text-sm text-gray-400">Central Park Arena<br/>Doors open at 14:00</p>
          </div>
          <div className="glass-panel p-6 rounded-xl flex flex-col items-center text-center glow-effect transition-all duration-300 hover:-translate-y-1">
            <Tent className="w-8 h-8 text-[#d1bcff] mb-3" />
            <h3 className="font-semibold mb-2">Camping</h3>
            <p className="text-sm text-gray-400">Dreamville opens Thursday<br/>Access with special pass only</p>
          </div>
          <div className="glass-panel p-6 rounded-xl flex flex-col items-center text-center glow-effect transition-all duration-300 hover:-translate-y-1">
            <Music className="w-8 h-8 text-[#00dbe9] mb-3" />
            <h3 className="font-semibold mb-2">Stages</h3>
            <p className="text-sm text-gray-400">6 Stages playing non-stop<br/>Check Lineup for schedules</p>
          </div>
        </div>

        <h2 className="text-2xl font-bold mb-6 text-white text-center">Frequently Asked Questions</h2>
        
        <div className="space-y-2">
          {faqs.map((faq, index) => (
            <FAQItem
              key={index}
              question={faq.question}
              answer={faq.answer}
              isOpen={openIndex === index}
              onClick={() => setOpenIndex(openIndex === index ? null : index)}
            />
          ))}
        </div>
      </div>
    </div>
  );
};
