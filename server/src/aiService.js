const axios = require('axios');
const { config } = require('./config');
const logger = require('./logger');

const GEMINI_BASE = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent';

async function getTheoryFeedback({ questionText, markingScheme, totalMarks, studentAnswer }) {
  const apiKey = config.geminiApiKey;
  if (!apiKey) {
    throw new Error('GEMINI_API_KEY not configured on server');
  }

  const prompt = `You are an experienced Ghana JHS examiner and educator. 
Evaluate the student's answer strictly based on the official marking scheme and award marks out of ${totalMarks}.
Be fair but strict. Provide a clear, encouraging, and educational explanation.

QUESTION: ${questionText}
OFFICIAL MARKING SCHEME: ${markingScheme}
TOTAL MARKS: ${totalMarks}
STUDENT ANSWER: ${studentAnswer}

Respond STRICTLY with a valid JSON object matching this schema exactly, with NO markdown formatting around it (no \`\`\`json):
{
  "marks_awarded": [number],
  "total_marks": [number],
  "percentage": [number],
  "grade": "A1" or "B2" or "B3" or "C4" or "C5" or "C6" or "D7" or "E8" or "F9",
  "feedback": {
    "strengths": ["string", "string"],
    "weaknesses": ["string", "string"],
    "corrections": ["string", "string"],
    "model_answer_summary": "string"
  },
  "encouragement": "string"
}`;

  try {
    const response = await axios.post(
      `${GEMINI_BASE}?key=${apiKey}`,
      {
        contents: [{ parts: [{ text: prompt }] }],
        generationConfig: {
          responseMimeType: 'application/json',
          temperature: 0.3,
        },
      },
      { timeout: 60000 }
    );

    const data = response.data;
    const textResult = data.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!textResult) throw new Error('Empty Gemini response');
    return JSON.parse(textResult);
  } catch (err) {
    logger.error('Gemini theory feedback failed', { error: err.message });
    throw err;
  }
}

async function getChatResponse({ userMessage, chatHistory }) {
  const apiKey = config.geminiApiKey;
  if (!apiKey) {
    throw new Error('GEMINI_API_KEY not configured on server');
  }

  const systemInstructionText = `You are JHS ExamBot, an encouraging and highly intelligent JHS study assistant for students in Ghana. Use simple, friendly terms. Help with study concepts from the Ghanaian BECE curriculum. Always give the most vivid, comprehensive, and best answer ever to the questions asked. Neatly arrange your answers with clear headers, beautiful bullet points, and clean spacing. Make it incredibly engaging, structured, and visually outstanding for a student. For math equations, exponents, fractions, and formulas, format them clearly using standard symbols and notation (e.g. use ² for squared, ³ for cubed, √ for square root, × for multiplication, ÷ for division). Put each step of mathematical workings on a new line and indent them to make it extremely clear and easy to follow. CRITICAL: NEVER use LaTeX, MathJax, or raw LaTeX mathematical formatting under any circumstances. Always use plain text with unicode superscripts/subscripts and standard symbols like ×, ÷, ², ³, √.`;

  const contents = [];
  for (const turn of chatHistory) {
    const role = turn.isUser ? 'user' : 'model';
    contents.push({ role, parts: [{ text: turn.message }] });
  }
  contents.push({ role: 'user', parts: [{ text: userMessage }] });

  try {
    const response = await axios.post(
      `${GEMINI_BASE}?key=${apiKey}`,
      {
        contents,
        systemInstruction: { parts: [{ text: systemInstructionText }] },
        generationConfig: { temperature: 0.7 },
      },
      { timeout: 60000 }
    );

    const data = response.data;
    const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!text) throw new Error('Empty Gemini response');
    return text;
  } catch (err) {
    logger.error('Gemini chat failed', { error: err.message });
    throw err;
  }
}

module.exports = { getTheoryFeedback, getChatResponse };
